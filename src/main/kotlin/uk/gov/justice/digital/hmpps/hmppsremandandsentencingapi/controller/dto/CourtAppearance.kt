package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate
import java.util.UUID

data class CourtAppearance(
  val appearanceUuid: UUID,
  val outcome: CourtAppearanceOutcome?,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val warrantId: String?,
  val warrantType: String,
  val taggedBail: Int?,
  val nextCourtAppearance: NextCourtAppearance?,
  val charges: List<Charge>,
  val overallSentenceLength: PeriodLength?,
  val overallConvictionDate: LocalDate?,
  val legacyData: CourtAppearanceLegacyData?,
) {
  companion object {

    val returnChargeStatuses: Set<EntityStatus> = setOf(EntityStatus.ACTIVE, EntityStatus.INACTIVE, EntityStatus.MERGED)

    fun from(courtAppearanceEntity: CourtAppearanceEntity): CourtAppearance = CourtAppearance(
      courtAppearanceEntity.appearanceUuid,
      courtAppearanceEntity.appearanceOutcome?.let { CourtAppearanceOutcome.from(it) },
      courtAppearanceEntity.courtCode,
      courtAppearanceEntity.courtCaseReference,
      courtAppearanceEntity.appearanceDate,
      courtAppearanceEntity.warrantId,
      courtAppearanceEntity.warrantType,
      courtAppearanceEntity.taggedBail,
      courtAppearanceEntity.nextCourtAppearance?.let { NextCourtAppearance.from(it) },
      courtAppearanceEntity.charges.filter { returnChargeStatuses.contains(it.statusId) }.map { Charge.from(it) },
      courtAppearanceEntity.periodLengths.firstOrNull { it.periodLengthType == PeriodLengthType.OVERALL_SENTENCE_LENGTH }?.let { PeriodLength.from(it) },
      courtAppearanceEntity.overallConvictionDate,
      courtAppearanceEntity.legacyData,
    )
  }
}
