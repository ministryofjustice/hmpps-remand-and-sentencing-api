package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource

@Service
class DpsDomainEventService(private val courtCaseDomainEventService: CourtCaseDomainEventService, private val courtAppearanceDomainEventService: CourtAppearanceDomainEventService, private val chargeDomainEventService: ChargeDomainEventService, private val sentenceDomainEventService: SentenceDomainEventService) {

  fun emitEvents(eventsMetadata: List<EventMetadata>) {
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
            EventSource.DPS,
          )
          EventType.SENTENCE_UPDATED -> sentenceDomainEventService.update(
            eventMetaData.prisonerId,
            eventMetaData.sentenceId!!,
            eventMetaData.chargeId!!,
            EventSource.DPS,
          )
          EventType.SENTENCE_DELETED -> sentenceDomainEventService.delete(
            eventMetaData.prisonerId,
            eventMetaData.sentenceId!!,
            eventMetaData.chargeId!!,
            EventSource.DPS,
          )
        }
      }
  }
}
