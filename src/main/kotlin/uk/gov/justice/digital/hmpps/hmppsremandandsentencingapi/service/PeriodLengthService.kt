package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import java.util.function.Consumer

@Service
class PeriodLengthService(
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
) {
  fun upsert(
    createPeriodLengthEntities: List<PeriodLengthEntity>,
    existingPeriodLengths: MutableSet<PeriodLengthEntity>,
    prisonerId: String,
    onCreateConsumer: Consumer<PeriodLengthEntity>,
    courtAppearanceId: String? = null, // courtAppearanceId and courtCaseId are optional because they're only used for the events which are only used by the SentenceServvice
    courtCaseId: String? = null,
  ): RecordResponse<EntityChangeStatus> {
    // The events to emit (sent in the response) are currently only used by the SentenceService (not used by the CourtAppearanceService)
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE

    existingPeriodLengths.forEach { existingPeriodLength ->
      val updatedPeriodLength = createPeriodLengthEntities.firstOrNull { it.periodLengthUuid == existingPeriodLength.periodLengthUuid }
      if (updatedPeriodLength != null) {
        if (!existingPeriodLength.isSame(updatedPeriodLength)) {
          updatedPeriodLength.legacyData = updatedPeriodLength.legacyData?.let { legacyData ->
            if (existingPeriodLength.periodLengthType != updatedPeriodLength.periodLengthType) {
              legacyData.sentenceTermCode = null
              legacyData.lifeSentence = null
            }
            legacyData
          }
          existingPeriodLength.updateFrom(updatedPeriodLength, serviceUserService.getUsername())
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingPeriodLength))
          entityChangeStatus = EntityChangeStatus.EDITED

          eventsToEmit.add(
            EventMetadataCreator.periodLengthEventMetadata(
              prisonerId = prisonerId,
              courtCaseId = courtCaseId.toString(),
              courtAppearanceId = courtAppearanceId.toString(),
              chargeId = existingPeriodLength.sentenceEntity?.charge?.chargeUuid.toString(),
              sentenceId = existingPeriodLength.sentenceEntity?.sentenceUuid.toString(),
              periodLengthId = existingPeriodLength.periodLengthUuid.toString(),
              eventType = EventType.PERIOD_LENGTH_UPDATED,
            ),
          )
        }
      } else {
        existingPeriodLength.delete(serviceUserService.getUsername())
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingPeriodLength))
        entityChangeStatus = EntityChangeStatus.DELETED

        eventsToEmit.add(
          EventMetadataCreator.periodLengthEventMetadata(
            prisonerId = prisonerId,
            courtCaseId = courtCaseId.toString(),
            courtAppearanceId = courtAppearanceId.toString(),
            chargeId = existingPeriodLength.sentenceEntity?.charge?.chargeUuid.toString(),
            sentenceId = existingPeriodLength.sentenceEntity?.sentenceUuid.toString(),
            periodLengthId = existingPeriodLength.periodLengthUuid.toString(),
            eventType = EventType.PERIOD_LENGTH_DELETED,
          ),
        )
      }
    }

    val toAddPeriodLengths = createPeriodLengthEntities
      .filter { toAddPeriodLength -> existingPeriodLengths.none { it.periodLengthUuid == toAddPeriodLength.periodLengthUuid } }
      .map {
        onCreateConsumer.accept(it)
        val savedPeriodLength = periodLengthRepository.save(it)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(savedPeriodLength))
        entityChangeStatus = EntityChangeStatus.CREATED

        eventsToEmit.add(
          EventMetadataCreator.periodLengthEventMetadata(
            prisonerId = prisonerId,
            courtCaseId = courtCaseId.toString(),
            courtAppearanceId = courtAppearanceId.toString(),
            chargeId = savedPeriodLength.sentenceEntity?.charge?.chargeUuid.toString(),
            sentenceId = savedPeriodLength.sentenceEntity?.sentenceUuid.toString(),
            periodLengthId = savedPeriodLength.periodLengthUuid.toString(),
            eventType = EventType.PERIOD_LENGTH_INSERTED,
          ),
        )
        savedPeriodLength
      }
    existingPeriodLengths.addAll(toAddPeriodLengths)
    return RecordResponse(entityChangeStatus, eventsToEmit)
  }
}
