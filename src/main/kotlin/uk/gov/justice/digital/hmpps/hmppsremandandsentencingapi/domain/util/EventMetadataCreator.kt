package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType

class EventMetadataCreator {
  companion object {
    fun courtCaseEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, null, null, null, null, eventType)

    fun courtAppearanceEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      courtAppearanceId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, courtAppearanceId, null, null, null, eventType)

    fun chargeEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      courtAppearanceId: String?,
      chargeId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, courtAppearanceId, chargeId, null, null, eventType)

    fun sentenceEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      chargeId: String,
      sentenceId: String,
      courtAppearanceId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, courtAppearanceId, chargeId, sentenceId, null, eventType)

    fun recallEventMetadata(
      prisonerId: String,
      recallId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, null, null, null, null, recallId, eventType)

    fun periodLengthEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      courtAppearanceId: String?,
      chargeId: String,
      sentenceId: String,
      periodLengthId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(
        prisonerId = prisonerId,
        courtCaseId = courtCaseId,
        courtAppearanceId = courtAppearanceId,
        chargeId = chargeId,
        sentenceId = sentenceId,
        recallId = null,
        eventType = eventType,
        periodLengthId = periodLengthId)
  }
}
