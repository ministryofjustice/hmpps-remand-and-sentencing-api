package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceSubtype
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import java.time.LocalDateTime
import java.time.LocalTime

data class PersonNextCourtAppearance(
  val caseReference: String?,
  val courtCode: String,
  val appearanceType: AppearanceType,
  val courtAppearanceSubType: CourtAppearanceSubtype?,
  val dateTime: LocalDateTime,
) {
  companion object {
    fun from(nextCourtAppearance: NextCourtAppearanceEntity, caseReference: String?): PersonNextCourtAppearance = PersonNextCourtAppearance(
      caseReference,
      nextCourtAppearance.courtCode,
      AppearanceType.from(nextCourtAppearance.appearanceType),
      nextCourtAppearance.courtAppearanceSubtype?.let { CourtAppearanceSubtype.from(it) },
      nextCourtAppearance.appearanceDate.atTime(nextCourtAppearance.appearanceTime ?: LocalTime.MIDNIGHT),
    )
  }
}
