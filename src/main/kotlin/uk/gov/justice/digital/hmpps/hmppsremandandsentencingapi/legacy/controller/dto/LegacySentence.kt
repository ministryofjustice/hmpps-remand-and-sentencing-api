package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class LegacySentence(
  val prisonerId: String,
  val courtCaseId: String,
  val chargeLifetimeUuid: UUID,
  val lifetimeUuid: UUID,
  val appearanceUuid: UUID,
  val active: Boolean,
  val sentenceCalcType: String,
  val sentenceCategory: String,
  val consecutiveToLifetimeUuid: UUID?,
  val chargeNumber: String?,
  val fineAmount: BigDecimal?,
  val sentenceStartDate: LocalDate,
  val returnToCustodyDate: LocalDate?,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): LegacySentence {
      val activeAppearances = sentenceEntity.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId == EntityStatus.ACTIVE }

      val courtCase = activeAppearances.maxBy { it.appearanceDate }.courtCase
      val firstSentenceAppearance = activeAppearances
        .filter { it.warrantType == "SENTENCING" }
        .minBy { it.appearanceDate }

      val latestRecall = sentenceEntity.latestRecall()
      val sentenceTypeAndCategory = getSentenceCalcTypeAndCategory(sentenceEntity, latestRecall)

      return LegacySentence(
        courtCase.prisonerId,
        courtCase.caseUniqueIdentifier,
        sentenceEntity.charge.chargeUuid,
        sentenceEntity.sentenceUuid,
        firstSentenceAppearance.appearanceUuid,
        sentenceEntity.statusId == EntityStatus.ACTIVE,
        sentenceTypeAndCategory.first,
        sentenceTypeAndCategory.second,
        sentenceEntity.consecutiveTo?.sentenceUuid,
        sentenceEntity.chargeNumber,
        sentenceEntity.fineAmount,
        firstSentenceAppearance.appearanceDate,
        latestRecall?.returnToCustodyDate,
      )
    }

    private fun getSentenceCalcTypeAndCategory(sentenceEntity: SentenceEntity, latestRecall: RecallEntity?): Pair<String, String> {
      return if (latestRecall == null) {
        (sentenceEntity.sentenceType?.nomisSentenceCalcType ?: sentenceEntity.legacyData!!.sentenceCalcType!!) to
          (sentenceEntity.sentenceType?.nomisCjaCode ?: sentenceEntity.legacyData!!.sentenceCategory!!)
      } else {
        if (LegacySentenceService.recallSentenceTypeBucketUuid == sentenceEntity.sentenceType?.sentenceTypeUuid) {
          val recallLegacyData = latestRecall.let { recall -> sentenceEntity.recallSentences.find { it.recall.id == recall.id }?.legacyData }
          if (recallLegacyData?.sentenceCalcType != null) {
            return recallLegacyData.sentenceCalcType to recallLegacyData.sentenceCategory!!
          } else {
            // A legacy recall has been recalled again in DPS and the sentence type has not been corrected
            throw IllegalStateException("Unknown legacy recall sentence type has been recalled again")
          }
        } else {
          latestRecall.recallType.toLegacySentenceType(sentenceEntity.sentenceType!!)
        }
      }
    }
  }
}
