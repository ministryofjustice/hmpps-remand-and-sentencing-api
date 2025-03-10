package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class MigrationCreateCharge(
  val chargeNOMISId: Long,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  var legacyData: ChargeLegacyData,
  val sentence: MigrationCreateSentence?,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonSetter(nulls = Nulls.SKIP)
  var merged: Boolean = false,
  val mergedFromCaseId: Long?,
  val mergedChargeNOMISId: Long?,
)
