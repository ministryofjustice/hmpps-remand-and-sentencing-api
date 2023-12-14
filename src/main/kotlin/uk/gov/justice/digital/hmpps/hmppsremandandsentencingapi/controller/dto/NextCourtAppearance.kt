package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import java.time.LocalDate

data class NextCourtAppearance(
  val appearanceDate: LocalDate,
  val courtCode: String,
  val appearanceType: String,
) {
  companion object {
    fun from(nextCourtAppearanceEntity: NextCourtAppearanceEntity): NextCourtAppearance {
      return NextCourtAppearance(
        nextCourtAppearanceEntity.appearanceDate,
        nextCourtAppearanceEntity.courtCode,
        nextCourtAppearanceEntity.appearanceType,
      )
    }
  }
}
