package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus

data class UpdateCourtCaseStatus(
  val status: CourtCaseEntityStatus,
  val reason: String?,
)
