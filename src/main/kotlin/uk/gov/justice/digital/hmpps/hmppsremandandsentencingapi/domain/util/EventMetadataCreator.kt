package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType

class EventMetadataCreator {
  companion object {
    fun courtCaseEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, null, null, null, eventType)

    fun courtAppearanceEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      courtAppearanceId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, courtAppearanceId, null, null, eventType)

    fun chargeEventMetadata(
      prisonerId: String,
      courtCaseId: String,
      courtAppearanceId: String?,
      chargeId: String,
      eventType: EventType,
    ): EventMetadata = EventMetadata(prisonerId, courtCaseId, courtAppearanceId, chargeId, null, eventType)
  }
}
