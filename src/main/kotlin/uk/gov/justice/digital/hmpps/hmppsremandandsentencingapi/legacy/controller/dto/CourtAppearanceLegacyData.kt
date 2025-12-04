package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CourtAppearanceLegacyData(
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
  val nextEventDateTime: LocalDateTime?,
  val appearanceTime: LocalTime?,
  val outcomeDispositionCode: String?,
  val outcomeConvictionFlag: Boolean?,
) {
  fun isSame(other: CourtAppearanceLegacyData?): Boolean = nomisOutcomeCode == other?.nomisOutcomeCode &&
    outcomeDescription == other?.outcomeDescription &&
    nextEventDateTime == other?.nextEventDateTime &&
    appearanceTime == other?.appearanceTime &&
    outcomeDispositionCode == other?.outcomeDispositionCode &&
    outcomeConvictionFlag == other?.outcomeConvictionFlag

  fun copyFrom(appearanceTime: LocalTime?): CourtAppearanceLegacyData = CourtAppearanceLegacyData(
    LocalDate.now().format(DateTimeFormatter.ISO_DATE),
    nomisOutcomeCode,
    outcomeDescription,
    nextEventDateTime,
    appearanceTime,
    outcomeDispositionCode,
    outcomeConvictionFlag,
  )
  companion object {
    fun from(appearanceTime: LocalTime): CourtAppearanceLegacyData = CourtAppearanceLegacyData(
      LocalDate.now().format(DateTimeFormatter.ISO_DATE),
      null,
      null,
      null,
      appearanceTime,
      null,
      null,
    )
  }
}
