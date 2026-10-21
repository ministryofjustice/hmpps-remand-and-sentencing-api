package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import java.time.LocalDate

data class F986EligibleCourtAppearanceOffence(
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
)
