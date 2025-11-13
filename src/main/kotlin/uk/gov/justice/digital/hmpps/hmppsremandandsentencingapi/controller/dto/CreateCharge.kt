package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.*

data class CreateCharge(
  val appearanceUuid: UUID?,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonSetter(nulls = Nulls.SKIP)
  var chargeUuid: UUID = UUID.randomUUID(),
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val outcomeUuid: UUID?,
  val terrorRelated: Boolean?,
  val sentence: CreateSentence?,
  var legacyData: ChargeLegacyData?,
  val prisonId: String,
  val replacedChargeUuid: UUID?,
)
