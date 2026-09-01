package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class NextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime?,
  val courtCode: String,
  val appearanceType: AppearanceType,
  val futureSkeletonAppearanceUuid: UUID?,
  val courtAppearanceSubType: CourtAppearanceSubtype?,
) {
  companion object {
    fun from(nextCourtAppearanceEntity: NextCourtAppearanceEntity): NextCourtAppearance = NextCourtAppearance(
      nextCourtAppearanceEntity.appearanceDate,
      nextCourtAppearanceEntity.appearanceTime,
      nextCourtAppearanceEntity.courtCode,
      AppearanceType.from(nextCourtAppearanceEntity.appearanceType),
      nextCourtAppearanceEntity.futureSkeletonAppearance.takeUnless { it.statusId == CourtAppearanceEntityStatus.DELETED }?.appearanceUuid,
      nextCourtAppearanceEntity.courtAppearanceSubtype?.let { CourtAppearanceSubtype.from(it) },
    )
  }
}
