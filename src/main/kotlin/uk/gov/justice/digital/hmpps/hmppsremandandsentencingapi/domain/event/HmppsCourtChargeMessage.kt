package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsCourtChargeMessage(
  val courtChargeId: String,
  val source: String = "DPS",
)
