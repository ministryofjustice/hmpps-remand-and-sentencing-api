package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate
import java.util.UUID

data class CreateCharge(
  val chargeUuid: UUID?,
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val outcome: String,
  val terrorRelated: Boolean?,
)
