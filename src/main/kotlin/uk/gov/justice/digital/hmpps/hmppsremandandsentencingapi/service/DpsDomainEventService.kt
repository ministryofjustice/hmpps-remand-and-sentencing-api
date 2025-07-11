package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource

@Service
class DpsDomainEventService(
  private val courtCaseDomainEventService: CourtCaseDomainEventService,
  private val courtAppearanceDomainEventService: CourtAppearanceDomainEventService,
  private val chargeDomainEventService: ChargeDomainEventService,
  private val sentenceDomainEventService: SentenceDomainEventService,
  private val recallDomainEventService: RecallDomainEventService,
  private val periodLengthDomainEventService: PeriodLengthDomainEventService,
) {

  fun emitEvents(eventsMetadata: Set<EventMetadata>) {
    eventsMetadata.sortedBy { it.eventType.order }
      .forEach { eventMetaData ->
        when (eventMetaData.eventType) {
          EventType.COURT_CASE_INSERTED -> courtCaseDomainEventService.create(
            eventMetaData.courtCaseId!!,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )
          EventType.COURT_CASE_UPDATED -> courtCaseDomainEventService.update(
            eventMetaData.courtCaseId!!,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )
          EventType.COURT_CASE_DELETED -> courtCaseDomainEventService.delete(
            eventMetaData.courtCaseId!!,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )
          EventType.LEGACY_COURT_CASE_REFERENCES_UPDATED -> courtCaseDomainEventService.legacyCaseReferencesUpdated(
            eventMetaData.courtCaseId!!,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )

          EventType.CHARGE_INSERTED -> chargeDomainEventService.create(
            eventMetaData.prisonerId,
            eventMetaData.chargeId!!,
            eventMetaData.courtCaseId!!,
            EventSource.DPS,
          )
          EventType.CHARGE_UPDATED -> chargeDomainEventService.update(
            eventMetaData.prisonerId,
            eventMetaData.chargeId!!,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId!!,
            EventSource.DPS,
          )
          EventType.CHARGE_DELETED -> chargeDomainEventService.delete(
            eventMetaData.prisonerId,
            eventMetaData.chargeId!!,
            eventMetaData.courtCaseId!!,
            EventSource.DPS,
          )
          EventType.COURT_APPEARANCE_INSERTED -> courtAppearanceDomainEventService.create(
            eventMetaData.prisonerId,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId!!,
            EventSource.DPS,
          )
          EventType.COURT_APPEARANCE_UPDATED -> courtAppearanceDomainEventService.update(
            eventMetaData.prisonerId,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId!!,
            EventSource.DPS,
          )
          EventType.COURT_APPEARANCE_DELETED -> courtAppearanceDomainEventService.delete(
            eventMetaData.prisonerId,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId!!,
            EventSource.DPS,
          )
          EventType.SENTENCE_INSERTED -> sentenceDomainEventService.create(
            eventMetaData.prisonerId,
            eventMetaData.sentenceId!!,
            eventMetaData.chargeId!!,
            eventMetaData.courtCaseId!!,
            eventMetaData.courtAppearanceId!!,
            EventSource.DPS,
          )
          EventType.SENTENCE_FIX_SINGLE_CHARGE_INSERTED -> sentenceDomainEventService.createFromFix(
            eventMetaData.prisonerId,
            eventMetaData.sentenceId!!,
            eventMetaData.originalSentenceId!!,
            eventMetaData.chargeId!!,
            eventMetaData.courtCaseId!!,
            eventMetaData.courtAppearanceId!!,
            EventSource.DPS,
          )
          EventType.SENTENCE_UPDATED -> sentenceDomainEventService.update(
            eventMetaData.prisonerId,
            eventMetaData.sentenceId!!,
            eventMetaData.chargeId!!,
            eventMetaData.courtCaseId!!,
            eventMetaData.courtAppearanceId!!,
            EventSource.DPS,
          )
          EventType.SENTENCE_DELETED -> sentenceDomainEventService.delete(
            eventMetaData.prisonerId,
            eventMetaData.sentenceId!!,
            eventMetaData.chargeId!!,
            eventMetaData.courtCaseId!!,
            eventMetaData.courtAppearanceId!!,
            EventSource.DPS,
          )

          EventType.RECALL_INSERTED -> recallDomainEventService.create(
            eventMetaData.prisonerId,
            eventMetaData.recallId!!,
            eventMetaData.sentenceIds!!,
            EventSource.DPS,
          )
          EventType.RECALL_UPDATED -> recallDomainEventService.update(
            eventMetaData.prisonerId,
            eventMetaData.recallId!!,
            eventMetaData.sentenceIds!!,
            eventMetaData.previousSentenceIds!!,
            EventSource.DPS,
          )
          EventType.RECALL_DELETED -> recallDomainEventService.delete(
            eventMetaData.prisonerId,
            eventMetaData.recallId!!,
            eventMetaData.sentenceIds!!,
            eventMetaData.previousRecallId,
            EventSource.DPS,
          )
          EventType.PERIOD_LENGTH_INSERTED -> periodLengthDomainEventService.create(
            prisonerId = eventMetaData.prisonerId,
            periodLengthId = eventMetaData.periodLengthId!!,
            sentenceId = eventMetaData.sentenceId!!,
            courtChargeId = eventMetaData.chargeId!!,
            courtCaseId = eventMetaData.courtCaseId!!,
            courtAppearanceId = eventMetaData.courtAppearanceId!!,
            source = EventSource.DPS,
          )
          EventType.PERIOD_LENGTH_UPDATED -> periodLengthDomainEventService.update(
            prisonerId = eventMetaData.prisonerId,
            periodLengthId = eventMetaData.periodLengthId!!,
            sentenceId = eventMetaData.sentenceId!!,
            courtChargeId = eventMetaData.chargeId!!,
            courtCaseId = eventMetaData.courtCaseId!!,
            courtAppearanceId = eventMetaData.courtAppearanceId!!,
            source = EventSource.DPS,
          )
          EventType.PERIOD_LENGTH_DELETED -> periodLengthDomainEventService.delete(
            prisonerId = eventMetaData.prisonerId,
            periodLengthId = eventMetaData.periodLengthId!!,
            sentenceId = eventMetaData.sentenceId!!,
            courtChargeId = eventMetaData.chargeId!!,
            courtCaseId = eventMetaData.courtCaseId!!,
            courtAppearanceId = eventMetaData.courtAppearanceId!!,
            source = EventSource.DPS,
          )

          EventType.METADATA_ONLY -> {}
        }
      }
  }
}
