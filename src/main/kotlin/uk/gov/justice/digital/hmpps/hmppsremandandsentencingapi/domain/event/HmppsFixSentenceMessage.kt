package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsFixSentenceMessage(
  val sentenceId: String,
  val originalSentenceId: String,
  val courtChargeId: String,
  val courtCaseId: String,
  val courtAppearanceId: String,
  val source: EventSource = EventSource.DPS,
)
