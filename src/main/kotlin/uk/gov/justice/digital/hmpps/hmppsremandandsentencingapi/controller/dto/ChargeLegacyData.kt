package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class ChargeLegacyData(
  val offenderChargeId: String?,
  val bookingId: String?,
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
)
