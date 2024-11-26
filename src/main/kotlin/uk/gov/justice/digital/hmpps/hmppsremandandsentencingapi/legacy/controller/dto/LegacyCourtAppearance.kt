package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.util.UUID

data class LegacyCourtAppearance(
  val lifetimeUuid: UUID,
  val courtCaseUuid: String,
  val prisonerId: String,
  val nomisOutcomeCode: String?,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val charges: List<LegacyCharge>,
  val nextCourtAppearance: LegacyNextCourtAppearance?,
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity, objectMapper: ObjectMapper): LegacyCourtAppearance {
      val legacyData = courtAppearanceEntity.legacyData?.let {
        objectMapper.treeToValue<CourtAppearanceLegacyData>(
          it,
          CourtAppearanceLegacyData::class.java,
        )
      }
      return LegacyCourtAppearance(
        courtAppearanceEntity.lifetimeUuid,
        courtAppearanceEntity.courtCase.caseUniqueIdentifier,
        courtAppearanceEntity.courtCase.prisonerId,
        legacyData?.nomisOutcomeCode ?: courtAppearanceEntity.appearanceOutcome?.nomisCode,
        courtAppearanceEntity.courtCode,
        courtAppearanceEntity.appearanceDate,
        courtAppearanceEntity.charges.filter { it.statusId == EntityStatus.ACTIVE }.map { chargeEntity -> LegacyCharge.from(chargeEntity, objectMapper) },
        courtAppearanceEntity.nextCourtAppearance?.let { LegacyNextCourtAppearance.from(it) },
      )
    }
  }
}
