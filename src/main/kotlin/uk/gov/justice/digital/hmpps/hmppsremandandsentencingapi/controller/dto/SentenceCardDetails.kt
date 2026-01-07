package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.MissingSentenceInformationDetails
import java.time.LocalDate
import java.util.UUID

data class SentenceCardDetails(
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val sentenceUuid: UUID,
  val chargeUuid: UUID,
  val countNumber: String?,
  val sentenceType: String,
  val periodLengths: List<PeriodLength>,
  val sentenceServeType: String,
  val convictedDate: LocalDate?,
) {
  companion object {
    fun fromList(sentenceRows: List<MissingSentenceInformationDetails>): SentenceCardDetails {
      val first = sentenceRows.first()

      val periodLengths = sentenceRows
        .filter { it.periodLengthUuid != null }
        .map { row ->
          PeriodLength(
            years = row.years,
            months = row.months,
            weeks = row.weeks,
            days = row.days,
            periodOrder = row.periodOrder!!,
            periodLengthType = PeriodLengthType.valueOf(row.periodLengthType!!),
            legacyData = null,
            periodLengthUuid = row.periodLengthUuid!!,
          )
        }

      return SentenceCardDetails(
        offenceCode = first.offenceCode,
        offenceStartDate = first.offenceStartDate,
        sentenceUuid = first.sentenceUuid,
        chargeUuid = first.chargeUuid,
        countNumber = first.countNumber,
        sentenceType = first.sentenceTypeDescription,
        periodLengths = periodLengths, // This is now the full list
        sentenceServeType = first.sentenceServeType,
        convictedDate = first.convictionDate,
      )
    }
  }
}
