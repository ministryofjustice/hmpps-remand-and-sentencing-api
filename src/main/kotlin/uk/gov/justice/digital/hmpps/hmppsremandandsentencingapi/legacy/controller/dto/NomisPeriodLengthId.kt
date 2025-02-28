package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class NomisPeriodLengthId(
  val offenderBookingId: Long,
  val sentenceSequence: Int,
  val termSequence: Int,
)
