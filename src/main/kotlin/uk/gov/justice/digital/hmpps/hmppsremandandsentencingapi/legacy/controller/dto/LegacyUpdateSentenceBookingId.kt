package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class LegacyUpdateSentenceBookingId(
  val bookingId: Long,
  val performedByUser: String?,
)
