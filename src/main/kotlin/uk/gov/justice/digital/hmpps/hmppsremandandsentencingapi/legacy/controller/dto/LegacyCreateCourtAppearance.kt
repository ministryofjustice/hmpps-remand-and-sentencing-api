package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class LegacyCreateCourtAppearance(
  val courtCaseUuid: String,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val legacyData: CourtAppearanceLegacyData,
  val performedByUser: String?,
) {
  fun getAppearanceDateTime(): LocalDateTime = appearanceDate.atTime(legacyData.appearanceTime ?: LocalTime.of(10, 0))
}
