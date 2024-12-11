package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import java.time.LocalDate
import java.time.LocalTime

data class NextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime?,
  val courtCode: String,
  val appearanceType: String,
) {
  companion object {
    fun from(nextCourtAppearanceEntity: NextCourtAppearanceEntity): NextCourtAppearance {
      return NextCourtAppearance(
        nextCourtAppearanceEntity.appearanceDate,
        nextCourtAppearanceEntity.appearanceTime,
        nextCourtAppearanceEntity.courtCode,
        nextCourtAppearanceEntity.appearanceType.description,
      )
    }
  }
}
