package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationSentenceId(
  val offenderBookingId: Long,
  val sequence: Int,
)
