package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.time.LocalDate
import java.util.UUID

data class CourtAppearance(
  val appearanceUuid: UUID,
  val lifetimeUuid: UUID,
  val outcome: String,
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
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity): CourtAppearance {
      return CourtAppearance(
        courtAppearanceEntity.appearanceUuid,
        courtAppearanceEntity.lifetimeUuid,
        courtAppearanceEntity.appearanceOutcome.outcomeName,
        courtAppearanceEntity.courtCode,
        courtAppearanceEntity.courtCaseReference,
        courtAppearanceEntity.appearanceDate,
        courtAppearanceEntity.warrantId,
        courtAppearanceEntity.warrantType,
        courtAppearanceEntity.taggedBail,
        courtAppearanceEntity.nextCourtAppearance?.let { NextCourtAppearance.from(it) },
        courtAppearanceEntity.charges.filter { it.statusId == EntityStatus.ACTIVE }.map { Charge.from(it) },
        courtAppearanceEntity.periodLengths.firstOrNull { it.periodLengthType == PeriodLengthType.OVERALL_SENTENCE_LENGTH }?.let { PeriodLength.from(it) },
        courtAppearanceEntity.overallConvictionDate,
      )
    }
  }
}
