package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate

data class CreateNextCourtAppearance(
  val appearanceDate: LocalDate,
  val courtCode: String,
  val appearanceType: String,
)
