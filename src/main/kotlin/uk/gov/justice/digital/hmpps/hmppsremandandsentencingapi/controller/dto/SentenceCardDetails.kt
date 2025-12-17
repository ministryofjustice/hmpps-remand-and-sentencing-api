package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.MissingSentenceInformationDetails
import java.time.LocalDate
import java.util.UUID

data class SentenceCardDetails(
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val sentenceUuid: UUID,
  val countNumber: String?,
  val sentenceType: String,
  val periodLengths: List<PeriodLength>,
  val sentenceServeType: String,
  val convictedDate: LocalDate?,
) {
  companion object {
    fun from(missingSentenceInformationDetails: MissingSentenceInformationDetails): SentenceCardDetails {
      val periodLengths = if (missingSentenceInformationDetails.periodLengthUuid != null) {
        listOf(
          PeriodLength(
            years = missingSentenceInformationDetails.years,
            months = missingSentenceInformationDetails.months,
            weeks = missingSentenceInformationDetails.weeks,
            days = missingSentenceInformationDetails.days,
            periodOrder = missingSentenceInformationDetails.periodOrder!!,
            periodLengthType = PeriodLengthType.valueOf(missingSentenceInformationDetails.periodLengthType!!),
            legacyData = null,
            periodLengthUuid = missingSentenceInformationDetails.periodLengthUuid,
          ),
        )
      } else {
        emptyList()
      }
      return SentenceCardDetails(
        offenceCode = missingSentenceInformationDetails.offenceCode,
        offenceStartDate = missingSentenceInformationDetails.offenceStartDate?.toLocalDate(),
        sentenceUuid = missingSentenceInformationDetails.sentenceUuid,
        countNumber = missingSentenceInformationDetails.countNumber,
        sentenceType = missingSentenceInformationDetails.sentenceTypeDescription,
        periodLengths = periodLengths,
        sentenceServeType = missingSentenceInformationDetails.sentenceServeType,
        convictedDate = missingSentenceInformationDetails.convictionDate?.toLocalDate(),
      )
    }
  }
}
