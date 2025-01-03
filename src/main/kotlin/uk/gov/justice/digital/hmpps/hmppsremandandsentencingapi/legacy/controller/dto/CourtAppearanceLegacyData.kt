package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CourtAppearanceLegacyData(
  val eventId: String?,
  val caseId: String?,
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
  val nextEventDateTime: LocalDateTime?,
  val appearanceTime: LocalTime?,
) {

  fun copyFrom(appearanceTime: LocalTime?): CourtAppearanceLegacyData {
    return CourtAppearanceLegacyData(
      eventId,
      caseId,
      LocalDate.now().format(DateTimeFormatter.ISO_DATE),
      nomisOutcomeCode,
      outcomeDescription,
      nextEventDateTime,
      appearanceTime,
    )
  }
  companion object {
    fun from(appearanceTime: LocalTime): CourtAppearanceLegacyData {
      return CourtAppearanceLegacyData(
        null,
        null,
        LocalDate.now().format(DateTimeFormatter.ISO_DATE),
        null,
        null,
        null,
        appearanceTime,
      )
    }
  }
}
