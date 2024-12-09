package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsCourtChargeMessage(
  val courtChargeId: String,
  val courtCaseId: String,
  val source: EventSource = EventSource.DPS,
  val courtAppearanceId: String? = null,
)
