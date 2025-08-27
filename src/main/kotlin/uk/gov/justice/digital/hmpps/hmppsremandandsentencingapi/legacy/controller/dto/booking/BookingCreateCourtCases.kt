package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

data class BookingCreateCourtCases(
  val prisonerId: String,
  val courtCases: List<BookingCreateCourtCase>,
)
