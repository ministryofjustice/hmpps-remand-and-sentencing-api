package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class ChargeLegacyData(
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
  val outcomeDispositionCode: String?,
  val outcomeConvictionFlag: Boolean?,
  val offenceDescription: String?,
) {
  fun isSame(other: ChargeLegacyData?): Boolean = nomisOutcomeCode == other?.nomisOutcomeCode &&
    outcomeDescription == other?.outcomeDescription &&
    outcomeDispositionCode == other?.outcomeDispositionCode &&
    outcomeConvictionFlag == other?.outcomeConvictionFlag &&
    offenceDescription == other?.offenceDescription
}
