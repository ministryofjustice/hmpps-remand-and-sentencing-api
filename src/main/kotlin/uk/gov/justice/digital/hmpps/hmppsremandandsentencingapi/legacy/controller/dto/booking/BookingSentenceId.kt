package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

data class BookingSentenceId(
  val offenderBookingId: Long,
  val sequence: Int,
)
