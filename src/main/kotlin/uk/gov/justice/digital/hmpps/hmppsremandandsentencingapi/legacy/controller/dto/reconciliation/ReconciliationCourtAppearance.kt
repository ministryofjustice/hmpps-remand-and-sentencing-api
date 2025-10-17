package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class ReconciliationCourtAppearance(
  val appearanceUuid: UUID,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime,
  val nomisOutcomeCode: String?,
  val legacyData: CourtAppearanceLegacyData?,
  val nextCourtAppearance: ReconciliationNextCourtAppearance?,
  val appearanceTypeUuid: UUID,
  val charges: List<ReconciliationCharge>,
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity, appearanceTypeUuid: UUID): ReconciliationCourtAppearance = ReconciliationCourtAppearance(
      courtAppearanceEntity.appearanceUuid,
      courtAppearanceEntity.courtCode,
      courtAppearanceEntity.appearanceDate,
      courtAppearanceEntity.legacyData?.appearanceTime ?: LocalTime.MIDNIGHT,
      courtAppearanceEntity.legacyData?.nomisOutcomeCode ?: courtAppearanceEntity.appearanceOutcome?.nomisCode,
      courtAppearanceEntity.legacyData,
      courtAppearanceEntity.nextCourtAppearance?.let { ReconciliationNextCourtAppearance.from(it) },
      appearanceTypeUuid,
      courtAppearanceEntity.appearanceCharges.filter { it.charge != null && it.charge!!.statusId != EntityStatus.DELETED }
        .map { ReconciliationCharge.from(it.charge!!) },

    )
  }
}
