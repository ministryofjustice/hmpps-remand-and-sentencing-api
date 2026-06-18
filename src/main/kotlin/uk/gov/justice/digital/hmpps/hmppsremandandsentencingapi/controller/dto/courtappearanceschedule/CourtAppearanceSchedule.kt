package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.CourtAppearanceUtils
import java.time.LocalDateTime
import java.util.UUID

data class CourtAppearanceSchedule(
  val id: UUID,
  val personIdentifier: String,
  val courtCode: String,
  val reason: AppearanceScheduleReason,
  val start: LocalDateTime,
  val isDuplicate: Boolean,
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity, nextCourtAppearance: NextCourtAppearanceEntity?): CourtAppearanceSchedule = CourtAppearanceSchedule(
      courtAppearanceEntity.appearanceUuid,
      courtAppearanceEntity.courtCase.prisonerId,
      courtAppearanceEntity.courtCode,
      AppearanceScheduleReason(CourtAppearanceUtils.getNOMISAppearanceTypeCode(courtAppearanceEntity, nextCourtAppearance)),
      courtAppearanceEntity.appearanceDate.atTime(CourtAppearanceUtils.getStartTime(courtAppearanceEntity, nextCourtAppearance)),
      courtAppearanceEntity.statusId == CourtAppearanceEntityStatus.DUPLICATE,
    )
  }
}
