package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsBreachMessage(
  val courtCaseId: String,
  val courtAppearanceIds: Set<String>,
  val chargeIds: Set<String>,
  val sentenceIds: List<String>,
  val periodLengthIds: Set<String>,
  val source: EventSource,
)
