package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SentenceToChainTo(
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val chargeUuid: UUID,
  val sentenceUuid: UUID,
  val countNumber: String?,
  val chargeLegacyData: ChargeLegacyData?,
  val value: BigDecimal? = null,
  val periodLengths: List<PeriodLength> = emptyList(),
) {
  companion object {
    fun from(consecutiveToSentenceRows: List<ConsecutiveToSentenceRow>): SentenceToChainTo {
      val firstRow = consecutiveToSentenceRows.first()
      val periodLengths = consecutiveToSentenceRows
        .filter { it.periodLengthUuid != null }
        .distinctBy { it.periodLengthUuid }
        .map {
          PeriodLength(
            years = it.years,
            months = it.months,
            weeks = it.weeks,
            days = it.days,
            periodOrder = it.periodOrder!!,
            periodLengthType = it.periodLengthType!!,
            legacyData = it.periodLengthLegacyData,
            periodLengthUuid = it.periodLengthUuid!!,
          )
        }
      return SentenceToChainTo(
        firstRow.offenceCode,
        firstRow.offenceStartDate,
        firstRow.offenceEndDate,
        firstRow.chargeUuid,
        firstRow.sentenceUuid,
        firstRow.countNumber,
        firstRow.chargeLegacyData,
        firstRow.value,
        periodLengths,
      )
    }
  }
}
