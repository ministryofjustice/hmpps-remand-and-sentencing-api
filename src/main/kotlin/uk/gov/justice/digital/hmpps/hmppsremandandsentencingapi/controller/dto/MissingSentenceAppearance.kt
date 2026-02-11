package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.MissingSentenceInformationDetails
import java.time.LocalDate

data class MissingSentenceAppearance(
  val appearanceUuid: String,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val sentences: List<SentenceCardDetails>,
) {
  companion object {
    fun from(
      details: List<MissingSentenceInformationDetails>,
    ): List<MissingSentenceAppearance> = details
      .groupBy { it.appearanceUuid }
      .values
      .map { appearanceDetails ->
        val first = appearanceDetails.first()

        MissingSentenceAppearance(
          appearanceUuid = first.appearanceUuid.toString(),
          courtCode = first.courtCode,
          courtCaseReference = first.courtCaseReference,
          appearanceDate = first.appearanceDate,
          sentences = appearanceDetails
            .groupBy { it.sentenceUuid }
            .values
            .map(SentenceCardDetails::fromList),
        )
      }
  }
}
