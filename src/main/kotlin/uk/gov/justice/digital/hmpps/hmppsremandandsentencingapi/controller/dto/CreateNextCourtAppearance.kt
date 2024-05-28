package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate
import java.time.LocalTime

data class CreateNextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime,
  val courtCode: String,
  val appearanceType: String,
)
