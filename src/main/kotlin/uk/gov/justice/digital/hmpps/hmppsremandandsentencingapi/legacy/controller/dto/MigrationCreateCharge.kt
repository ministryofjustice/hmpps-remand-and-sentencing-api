package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.math.BigDecimal
import java.time.LocalDate

data class MigrationCreateCharge(
  val chargeNOMISId: Long,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  var legacyData: ChargeLegacyData,
  val sentence: MigrationCreateSentence?,
  val mergedFromCaseId: Long?,
  val mergedFromDate: LocalDate?,
  val value: BigDecimal? = null,
)
