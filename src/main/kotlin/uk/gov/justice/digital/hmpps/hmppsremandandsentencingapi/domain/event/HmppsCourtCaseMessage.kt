package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsCourtCaseMessage(
  val courtCaseId: String,
  val source: EventSource = EventSource.DPS,
)
