package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person

data class PersonCourtCaseCount(
  val suppliedBookingCount: Long,
  val otherBookingCount: Long,
)
