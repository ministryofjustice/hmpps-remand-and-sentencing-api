package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsPeriodLengthMessage(
  val periodLengthId: String,
  val sentenceId: String,
  val courtChargeId: String,
  val courtCaseId: String,
  val courtAppearanceId: String,
  val source: EventSource = EventSource.DPS,
)
