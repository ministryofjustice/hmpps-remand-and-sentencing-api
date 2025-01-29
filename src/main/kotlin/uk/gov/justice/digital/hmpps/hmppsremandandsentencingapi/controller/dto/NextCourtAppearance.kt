package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import java.time.LocalDate
import java.time.LocalTime

data class NextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime?,
  val courtCode: String,
  val appearanceType: AppearanceType,
) {
  companion object {
    fun from(nextCourtAppearanceEntity: NextCourtAppearanceEntity): NextCourtAppearance = NextCourtAppearance(
      nextCourtAppearanceEntity.appearanceDate,
      nextCourtAppearanceEntity.appearanceTime,
      nextCourtAppearanceEntity.courtCode,
      AppearanceType.from(nextCourtAppearanceEntity.appearanceType),
    )
  }
}
