package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsCourtAppearanceMessage(
  val courtAppearanceId: String,
  val source: String = "DPS",
)