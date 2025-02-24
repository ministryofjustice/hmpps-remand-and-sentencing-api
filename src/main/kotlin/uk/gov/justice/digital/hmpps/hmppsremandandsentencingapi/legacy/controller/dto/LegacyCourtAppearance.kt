package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class LegacyCourtAppearance(
  val lifetimeUuid: UUID,
  val courtCaseUuid: String,
  val prisonerId: String,
  val nomisOutcomeCode: String?,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime,
  val charges: List<LegacyCharge>,
  val nextCourtAppearance: LegacyNextCourtAppearance?,
) {
  companion object {

    val returnChargeStatuses: Set<EntityStatus> = setOf(EntityStatus.ACTIVE, EntityStatus.INACTIVE)

    fun from(courtAppearanceEntity: CourtAppearanceEntity): LegacyCourtAppearance = LegacyCourtAppearance(
      courtAppearanceEntity.appearanceUuid,
      courtAppearanceEntity.courtCase.caseUniqueIdentifier,
      courtAppearanceEntity.courtCase.prisonerId,
      courtAppearanceEntity.legacyData?.nomisOutcomeCode ?: courtAppearanceEntity.appearanceOutcome?.nomisCode,
      courtAppearanceEntity.courtCode,
      courtAppearanceEntity.appearanceDate,
      courtAppearanceEntity.legacyData?.appearanceTime ?: LocalTime.MIDNIGHT,
      courtAppearanceEntity.charges.filter { returnChargeStatuses.contains(it.statusId) }.map { chargeEntity -> LegacyCharge.from(chargeEntity) },
      courtAppearanceEntity.nextCourtAppearance?.let { LegacyNextCourtAppearance.from(it) },
    )
  }
}
