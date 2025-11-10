package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import java.util.function.Consumer
import kotlin.toString

@Service
class PeriodLengthService(
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
) {
  fun create(
    createPeriodLengthEntities: List<PeriodLengthEntity>,
    existingPeriodLengths: MutableSet<PeriodLengthEntity>,
    prisonerId: String,
    onCreateConsumer: Consumer<PeriodLengthEntity>,
    courtAppearanceId: String? = null,
    courtCaseId: String? = null,
    shouldGenerateEvents: Boolean = false,
  ): RecordResponse<EntityChangeStatus> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE

    val toAddPeriodLengths = createPeriodLengthEntities
      .filter { new -> existingPeriodLengths.none { it.periodLengthUuid == new.periodLengthUuid } }
      .map {
        onCreateConsumer.accept(it)
        val saved = periodLengthRepository.save(it)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(saved))
        entityChangeStatus = EntityChangeStatus.CREATED
        if (shouldGenerateEvents) {
          eventsToEmit.add(
            EventMetadataCreator.periodLengthEventMetadata(
              prisonerId,
              courtCaseId.toString(),
              courtAppearanceId.toString(),
              saved.sentenceEntity?.charge?.chargeUuid.toString(),
              saved.sentenceEntity?.sentenceUuid.toString(),
              saved.periodLengthUuid.toString(),
              EventType.PERIOD_LENGTH_INSERTED,
            ),
          )
        }
        saved
      }

    existingPeriodLengths.addAll(toAddPeriodLengths)
    return RecordResponse(entityChangeStatus, eventsToEmit)
  }

  fun update(
    updatedPeriodLengths: List<PeriodLengthEntity>,
    existingPeriodLengths: MutableSet<PeriodLengthEntity>,
    prisonerId: String,
    courtAppearanceId: String? = null,
    courtCaseId: String? = null,
    shouldGenerateEvents: Boolean = false,
  ): RecordResponse<EntityChangeStatus> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE

    existingPeriodLengths.forEach { existing ->
      val updated = updatedPeriodLengths.firstOrNull { it.periodLengthUuid == existing.periodLengthUuid }
      if (updated != null && !existing.isSame(updated)) {
        updated.legacyData = updated.legacyData?.let { legacyData ->
          if (existing.periodLengthType != updated.periodLengthType) {
            legacyData.sentenceTermCode = null
            legacyData.lifeSentence = null
          }
          legacyData
        }

        existing.updateFrom(updated, serviceUserService.getUsername())
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existing))
        entityChangeStatus = EntityChangeStatus.EDITED
        if (shouldGenerateEvents) {
          eventsToEmit.add(
            EventMetadataCreator.periodLengthEventMetadata(
              prisonerId,
              courtCaseId.toString(),
              courtAppearanceId.toString(),
              existing.sentenceEntity?.charge?.chargeUuid.toString(),
              existing.sentenceEntity?.sentenceUuid.toString(),
              existing.periodLengthUuid.toString(),
              EventType.PERIOD_LENGTH_UPDATED,
            ),
          )
        }
      }
    }

    return RecordResponse(entityChangeStatus, eventsToEmit)
  }

  fun delete(
    createPeriodLengthEntities: List<PeriodLengthEntity>,
    existingPeriodLengths: MutableSet<PeriodLengthEntity>,
    prisonerId: String,
    courtAppearanceId: String? = null,
    courtCaseId: String? = null,
    shouldGenerateEvents: Boolean = false,
  ): RecordResponse<List<PeriodLengthEntity>> {
    val eventsToEmit = mutableSetOf<EventMetadata>()

    val toDelete = existingPeriodLengths.filter { existing ->
      createPeriodLengthEntities.none { it.periodLengthUuid == existing.periodLengthUuid } && existing.statusId != PeriodLengthEntityStatus.DELETED
    }

    toDelete.forEach { existing ->
      val deletedPeriodLength = delete(existing, prisonerId, courtCaseId!!, courtAppearanceId!!)
      if (shouldGenerateEvents) {
        eventsToEmit.addAll(deletedPeriodLength.eventsToEmit)
      }
    }

    return RecordResponse(toDelete, eventsToEmit)
  }

  fun delete(periodLength: PeriodLengthEntity, prisonerId: String, courtCaseId: String, courtAppearanceId: String): RecordResponse<PeriodLengthEntity> {
    val changeStatus = if (periodLength.statusId == PeriodLengthEntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    val eventsToEmit = mutableSetOf<EventMetadata>()
    periodLength.delete(serviceUserService.getUsername())

    if (changeStatus == EntityChangeStatus.DELETED) {
      periodLengthHistoryRepository.save(periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength)))
      eventsToEmit.add(
        EventMetadataCreator.periodLengthEventMetadata(
          prisonerId,
          courtCaseId,
          courtAppearanceId,
          periodLength.sentenceEntity?.charge?.chargeUuid.toString(),
          periodLength.sentenceEntity?.sentenceUuid.toString(),
          periodLength.periodLengthUuid.toString(),
          EventType.PERIOD_LENGTH_DELETED,
        ),
      )
    }
    return RecordResponse(periodLength, eventsToEmit)
  }
}
