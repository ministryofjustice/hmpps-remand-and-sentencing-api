package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

data class MergeSentenceId(
  val offenderBookingId: Long,
  val sequence: Int,
)
