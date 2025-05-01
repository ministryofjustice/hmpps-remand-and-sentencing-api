package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class LegacyPeriodLengthService(
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
  private val sentenceRepository: SentenceRepository,
) {


  fun create(periodLength: LegacyCreatePeriodLength): RecordResponse<LegacyPeriodLengthCreatedResponse> {
    val eventsToEmit = mutableSetOf<EventMetadata>()

    val sentenceEntity = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(periodLength.sentenceUuid)
      ?: throw EntityNotFoundException("No sentence found at ${periodLength.sentenceUuid}")

    val sentenceCalcType = requireNotNull(sentenceEntity.sentenceType?.nomisSentenceCalcType) {
      "Sentence calculation type is required"
    }

    val isManyCharges = sentenceEntity.charge.appearanceCharges.size > 1
    val periodLengthEntity = PeriodLengthEntity.from(
      periodLength,
      sentenceCalcType,
      serviceUserService.getUsername(),
      isManyCharges
    )

    val savedPeriodLength = periodLengthRepository.save(periodLengthEntity)
    periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(savedPeriodLength))

    createEventMetadata(savedPeriodLength, sentenceEntity)?.let { eventsToEmit.add(it) }

    return RecordResponse(
      LegacyPeriodLengthCreatedResponse(
        periodLengthUuid = savedPeriodLength.periodLengthUuid,
        sentenceUuid = periodLength.sentenceUuid,
        chargeUuid = sentenceEntity.charge.chargeUuid,
        appearanceUuid = savedPeriodLength.appearanceEntity?.appearanceUuid,
        courtCaseId = savedPeriodLength.appearanceEntity?.courtCase?.id.toString(),
        prisonerId = savedPeriodLength.appearanceEntity?.courtCase?.prisonerId,
        sentenceTermNOMISId = periodLength.periodLengthId
      ),
      eventsToEmit
    )
  }

  private fun createEventMetadata(
    savedPeriodLength: PeriodLengthEntity,
    sentenceEntity: SentenceEntity
  ): EventMetadata? {
    val prisonerId = savedPeriodLength.appearanceEntity?.courtCase?.prisonerId ?: return null
    return EventMetadataCreator.periodLengthEventMetadata(
      prisonerId = prisonerId,
      courtCaseId = savedPeriodLength.appearanceEntity?.courtCase?.id.toString(),
      courtAppearanceId = savedPeriodLength.appearanceEntity?.appearanceUuid.toString(),
      chargeId = sentenceEntity.charge.chargeUuid.toString(),
      sentenceId = sentenceEntity.sentenceUuid.toString(),
      periodLengthId = savedPeriodLength.periodLengthUuid.toString(),
      eventType = EventType.PERIOD_LENGTH_INSERTED
    )
  }


  fun upsert(
    createPeriodLengthEntities: Map<NomisPeriodLengthId, PeriodLengthEntity>,
    sentenceEntity: SentenceEntity,
  ): Pair<EntityChangeStatus, Map<NomisPeriodLengthId, PeriodLengthEntity>> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingPeriodLengths = sentenceEntity.periodLengths
    existingPeriodLengths.forEach { existingPeriodLength ->
      val updatedPeriodLength =
        createPeriodLengthEntities.firstNotNullOfOrNull { if (it.value.periodLengthUuid == existingPeriodLength.periodLengthUuid) it.value else null }
      if (updatedPeriodLength != null) {
        if (!existingPeriodLength.isSame(updatedPeriodLength)) {
          existingPeriodLength.updateFrom(updatedPeriodLength, serviceUserService.getUsername())
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingPeriodLength))
          entityChangeStatus = EntityChangeStatus.EDITED
        }
      } else {
        existingPeriodLength.delete(serviceUserService.getUsername())
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingPeriodLength))
        EntityChangeStatus.DELETED
      }
    }
    val toAddPeriodLengths =
      createPeriodLengthEntities.filter { (_, toAddPeriodLength) -> existingPeriodLengths.none { it.periodLengthUuid == toAddPeriodLength.periodLengthUuid } }
        .mapValues { (_, toAddPeriodLength) ->
          toAddPeriodLength.sentenceEntity = sentenceEntity
          val savedPeriodLength = periodLengthRepository.save(toAddPeriodLength)
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(savedPeriodLength))
          EntityChangeStatus.CREATED
          savedPeriodLength
        }
    existingPeriodLengths.addAll(toAddPeriodLengths.values)
    return entityChangeStatus to toAddPeriodLengths
  }

  fun delete(periodLength: PeriodLengthEntity) {
    periodLength.delete(serviceUserService.getUsername())
    periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength))
  }
}
