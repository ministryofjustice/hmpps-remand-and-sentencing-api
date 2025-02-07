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
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, null, chargeId, sentenceId, null, eventType)

    fun recallEventMetadata(
      prisonerId: String,
      recallId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, null, null, null, null, recallId, eventType)
  }
}
