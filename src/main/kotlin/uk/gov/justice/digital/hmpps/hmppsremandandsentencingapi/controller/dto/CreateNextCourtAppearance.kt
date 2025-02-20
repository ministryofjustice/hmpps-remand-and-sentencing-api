package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class CreateNextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime?,
  val courtCode: String,
  val appearanceTypeUuid: UUID,
  val prisonId: String,
)
