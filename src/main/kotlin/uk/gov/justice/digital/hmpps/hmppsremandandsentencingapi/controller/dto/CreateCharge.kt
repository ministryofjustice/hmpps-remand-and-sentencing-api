package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class CreateCharge(
  val chargeUuid: UUID?,
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val outcomeUuid: UUID?,
  val terrorRelated: Boolean?,
  val sentence: CreateSentence?,
  val legacyData: ChargeLegacyData?,
)
