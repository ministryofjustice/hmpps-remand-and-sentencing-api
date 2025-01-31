package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class EventMetadata(
  val prisonerId: String,
  val courtCaseId: String?,
  val courtAppearanceId: String?,
  val chargeId: String?,
  val sentenceId: String?,
  val recallId: String?,
  val eventType: EventType,
)
