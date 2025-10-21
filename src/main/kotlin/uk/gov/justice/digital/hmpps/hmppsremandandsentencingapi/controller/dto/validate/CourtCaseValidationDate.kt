package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.validate

import java.time.LocalDate

data class CourtCaseValidationDate(
  val offenceDate: LocalDate?,
  val latestRemandAppearanceDate: LocalDate?,
  val latestSentenceAppearanceDate: LocalDate?,
)
