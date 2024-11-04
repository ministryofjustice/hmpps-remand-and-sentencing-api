package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class CreateCharge(
  val appearanceUuid: UUID?,
  @JsonSetter(nulls = Nulls.SKIP)
  var chargeUuid: UUID = UUID.randomUUID(),
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val outcomeUuid: UUID?,
  val terrorRelated: Boolean?,
  val sentence: CreateSentence?,
  val legacyData: ChargeLegacyData?,
)
