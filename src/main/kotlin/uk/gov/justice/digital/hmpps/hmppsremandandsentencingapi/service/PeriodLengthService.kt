package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.CourtCaseHierarchyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.LinkBreachSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyPeriodLengthService
import java.util.function.Consumer
import kotlin.toString

@Service
class PeriodLengthService(
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
  private val sentenceRepository: SentenceRepository,
  private val legacyPeriodLengthService: LegacyPeriodLengthService,
) {
  fun create(
    createPeriodLengthEntities: List<PeriodLengthEntity>,
    existingPeriodLengths: MutableSet<PeriodLengthEntity>,
    onCreateConsumer: Consumer<PeriodLengthEntity>,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): List<RecordResponse<PeriodLengthEntity>> {
    val toAddPeriodLengths = createPeriodLengthEntities
      .filter { new -> existingPeriodLengths.none { it.periodLengthUuid == new.periodLengthUuid } }
      .map {
        val periodLength = courtCaseHierarchyData.courtAppearancePeriodLengths.firstOrNull { courtAppearancePeriodLength -> courtAppearancePeriodLength.periodLengthUuid == it.periodLengthUuid } ?: it
        val eventsToEmit = mutableSetOf<EventMetadata>()
        onCreateConsumer.accept(periodLength)
        val linkBreachSentence = linkSentenceWithBreachOfSupervisionAppearancePeriodLength(courtCaseHierarchyData.courtCaseId!!, periodLength)
        val saved = periodLengthRepository.save(periodLength)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(saved, ChangeSource.DPS))
        if (saved.sentenceEntity != null) {
          eventsToEmit.add(
            EventMetadataCreator.periodLengthEventMetadata(
              courtCaseHierarchyData.prisonerId,
              courtCaseHierarchyData.courtCaseId!!,
              linkBreachSentence?.appearanceUuid?.toString() ?: courtCaseHierarchyData.courtAppearanceUuid.toString(),
              linkBreachSentence?.chargeUuid?.toString() ?: saved.sentenceEntity?.charge?.chargeUuid.toString(),
              saved.sentenceEntity?.sentenceUuid.toString(),
              saved.periodLengthUuid.toString(),
              EventType.PERIOD_LENGTH_INSERTED,
              courtCaseHierarchyData.isBreach,
            ),
          )
        }
        RecordResponse(saved, eventsToEmit)
      }
    return toAddPeriodLengths
  }

  private fun linkSentenceWithBreachOfSupervisionAppearancePeriodLength(courtCaseUuid: String, periodLengthEntity: PeriodLengthEntity): LinkBreachSentence? {
    var linkBreachSentence: LinkBreachSentence? = null
    if (periodLengthEntity.appearanceEntity != null && periodLengthEntity.sentenceEntity == null && periodLengthEntity.periodLengthType == PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS) {
      linkBreachSentence = sentenceRepository.findLinkableBreachSentenceId(courtCaseUuid)
      val sentenceEntity = sentenceRepository.findByIdOrNull(linkBreachSentence.sentenceId)
      periodLengthEntity.sentenceEntity = sentenceEntity
    }

    return linkBreachSentence
  }

  fun update(
    updatedPeriodLengths: List<PeriodLengthEntity>,
    existingPeriodLengths: MutableSet<PeriodLengthEntity>,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<EntityChangeStatus> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE

    existingPeriodLengths.forEach { existing ->
      val updated = updatedPeriodLengths.firstOrNull { it.periodLengthUuid == existing.periodLengthUuid }
      if (updated != null) {
        val nomisMappingDifferent = legacyPeriodLengthService.isNomisMappingDifferent(existing, courtCaseHierarchyData.existingSentenceType, updated, courtCaseHierarchyData.updatedSentenceType)
        if (!existing.isSame(updated)) {
          updated.legacyData = updated.legacyData?.let { legacyData ->
            if (existing.periodLengthType != updated.periodLengthType) {
              legacyData.sentenceTermCode = null
              legacyData.lifeSentence = null
            }
            legacyData
          }

          existing.updateFrom(updated, serviceUserService.getUsername())
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existing, ChangeSource.DPS))
          entityChangeStatus = EntityChangeStatus.EDITED
        }

        if ((entityChangeStatus == EntityChangeStatus.EDITED || nomisMappingDifferent) && existing.sentenceEntity != null) {
          eventsToEmit.add(
            EventMetadataCreator.periodLengthEventMetadata(
              courtCaseHierarchyData.prisonerId,
              courtCaseHierarchyData.courtCaseId ?: existing.sentenceEntity!!.charge.appearanceCharges.first { appearanceWarrantTypes.contains(it.appearance!!.warrantType) }.appearance!!.courtCase.caseUniqueIdentifier,
              courtCaseHierarchyData.courtAppearanceUuid?.toString() ?: existing.sentenceEntity!!.charge.appearanceCharges.first { appearanceWarrantTypes.contains(it.appearance!!.warrantType) }.appearance!!.appearanceUuid.toString(),
              existing.sentenceEntity?.charge?.chargeUuid.toString(),
              existing.sentenceEntity?.sentenceUuid.toString(),
              existing.periodLengthUuid.toString(),
              EventType.PERIOD_LENGTH_UPDATED,
              courtCaseHierarchyData.isBreach,
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
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<List<PeriodLengthEntity>> {
    val eventsToEmit = mutableSetOf<EventMetadata>()

    val toDelete = existingPeriodLengths.filter { existing ->
      createPeriodLengthEntities.none { it.periodLengthUuid == existing.periodLengthUuid } && existing.statusId != PeriodLengthEntityStatus.DELETED
    }

    toDelete.forEach { existing ->
      val deletedPeriodLength = delete(existing, courtCaseHierarchyData)
      eventsToEmit.addAll(deletedPeriodLength.eventsToEmit)
    }

    return RecordResponse(toDelete, eventsToEmit)
  }

  fun delete(periodLength: PeriodLengthEntity, courtCaseHierarchyData: CourtCaseHierarchyData): RecordResponse<PeriodLengthEntity> {
    val changeStatus = if (periodLength.statusId == PeriodLengthEntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    val eventsToEmit = mutableSetOf<EventMetadata>()
    periodLength.delete(serviceUserService.getUsername())

    if (changeStatus == EntityChangeStatus.DELETED) {
      periodLengthHistoryRepository.save(periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength, ChangeSource.DPS)))
      if (periodLength.sentenceEntity != null) {
        eventsToEmit.add(
          EventMetadataCreator.periodLengthEventMetadata(
            courtCaseHierarchyData.prisonerId,
            courtCaseHierarchyData.courtCaseId ?: periodLength.sentenceEntity!!.charge.appearanceCharges.first { appearanceWarrantTypes.contains(it.appearance!!.warrantType) }.appearance!!.courtCase.caseUniqueIdentifier,
            courtCaseHierarchyData.courtAppearanceUuid?.toString() ?: periodLength.sentenceEntity!!.charge.appearanceCharges.first { appearanceWarrantTypes.contains(it.appearance!!.warrantType) }.appearance!!.appearanceUuid.toString(),
            periodLength.sentenceEntity?.charge?.chargeUuid.toString(),
            periodLength.sentenceEntity?.sentenceUuid.toString(),
            periodLength.periodLengthUuid.toString(),
            EventType.PERIOD_LENGTH_DELETED,
            courtCaseHierarchyData.isBreach,
          ),
        )
      }
    }
    return RecordResponse(periodLength, eventsToEmit)
  }

  companion object {
    val appearanceWarrantTypes = setOf("SENTENCING", "BREACH_OF_IMPRISONABLE_OFFENCE")
  }
}
