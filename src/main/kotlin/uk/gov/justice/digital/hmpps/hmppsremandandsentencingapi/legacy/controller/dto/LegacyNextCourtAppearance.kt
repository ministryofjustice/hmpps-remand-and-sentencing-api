package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import java.time.LocalDate
import java.time.LocalTime

data class LegacyNextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime?,
  val courtId: String,
  val appearanceType: String,
) {
  companion object {
    fun from(nextCourtAppearanceEntity: NextCourtAppearanceEntity): LegacyNextCourtAppearance {
      return LegacyNextCourtAppearance(
        nextCourtAppearanceEntity.appearanceDate,
        nextCourtAppearanceEntity.appearanceTime,
        nextCourtAppearanceEntity.courtCode,
        nextCourtAppearanceEntity.appearanceType,
      )
    }
  }
}
