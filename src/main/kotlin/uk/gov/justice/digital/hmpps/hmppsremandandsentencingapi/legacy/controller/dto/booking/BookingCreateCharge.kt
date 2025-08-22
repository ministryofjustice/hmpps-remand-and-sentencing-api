package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate

data class BookingCreateCharge(
  val chargeNOMISId: Long,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  var legacyData: ChargeLegacyData,
  val sentence: BookingCreateSentence?,
  val mergedFromCaseId: Long?,
  val mergedFromDate: LocalDate?,
)
