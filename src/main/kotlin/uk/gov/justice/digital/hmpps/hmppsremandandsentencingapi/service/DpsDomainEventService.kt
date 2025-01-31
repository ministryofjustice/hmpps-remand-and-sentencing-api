package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource

@Service
class DpsDomainEventService(private val courtCaseDomainEventService: CourtCaseDomainEventService, private val courtAppearanceDomainEventService: CourtAppearanceDomainEventService) {

  fun emitEvents(eventsMetadata: List<EventMetadata>) {
    eventsMetadata.sortedBy { it.eventType.order }
      .forEach { eventMetaData ->
        when (eventMetaData.eventType) {
          EventType.COURT_CASE_INSERTED -> courtCaseDomainEventService.create(
            eventMetaData.courtCaseId,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )
          EventType.COURT_CASE_UPDATED -> courtCaseDomainEventService.update(
            eventMetaData.courtCaseId,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )
          EventType.COURT_CASE_DELETED -> courtCaseDomainEventService.delete(
            eventMetaData.courtCaseId,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )
          EventType.LEGACY_COURT_CASE_REFERENCES_UPDATED -> courtCaseDomainEventService.legacyCaseReferencesUpdated(
            eventMetaData.courtCaseId,
            eventMetaData.prisonerId,
            EventSource.DPS,
          )

          EventType.CHARGE_INSERTED -> TODO()
          EventType.CHARGE_UPDATED -> TODO()
          EventType.CHARGE_DELETED -> TODO()
          EventType.COURT_APPEARANCE_INSERTED -> courtAppearanceDomainEventService.create(
            eventMetaData.prisonerId,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId,
            EventSource.DPS,
          )
          EventType.COURT_APPEARANCE_UPDATED -> courtAppearanceDomainEventService.update(
            eventMetaData.prisonerId,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId,
            EventSource.DPS,
          )
          EventType.COURT_APPEARANCE_DELETED -> courtAppearanceDomainEventService.delete(
            eventMetaData.prisonerId,
            eventMetaData.courtAppearanceId!!,
            eventMetaData.courtCaseId,
            EventSource.DPS,
          )
          EventType.SENTENCE_INSERTED -> TODO()
          EventType.SENTENCE_UPDATED -> TODO()
        }
      }
  }
}
