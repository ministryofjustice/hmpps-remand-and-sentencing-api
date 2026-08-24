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
  val originalSentenceId: String? = null,
  val isOnFutureAppearance: Boolean? = null,
  val isBreach: Boolean? = null,
  val courtAppearanceIds: Set<String>? = null,
  val chargeIds: Set<String>? = null,
  val periodLengthIds: Set<String>? = null,
) {
  fun isBreach(): Boolean = isBreach ?: false
}
