package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsSentenceMessage(
  val sentenceId: String,
  val courtChargeId: String,
  val source: EventSource = EventSource.DPS,
)
