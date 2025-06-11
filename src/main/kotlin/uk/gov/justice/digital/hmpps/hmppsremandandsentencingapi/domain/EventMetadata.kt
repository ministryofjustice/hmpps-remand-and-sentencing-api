package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class EventMetadata(
  val prisonerId: String,
  val courtCaseId: String?,
  val courtAppearanceId: String?,
  val chargeId: String?,
  val sentenceId: String?,
  val recallId: String?,
  val eventType: EventType,
  val periodLengthId: String? = null,
  val sentenceIds: List<String>? = null,
  val previousRecallId: String? = null,
  val previousSentenceIds: List<String>? = null,
)
