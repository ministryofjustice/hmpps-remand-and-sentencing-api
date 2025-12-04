package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyRecallService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.SentenceUtils
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.RecallService.Companion.unknownPreRecallSentenceTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

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
        .filter { it.statusId == CourtAppearanceEntityStatus.ACTIVE }

      val courtCase = activeAppearances.maxBy { it.appearanceDate }.courtCase
      val sentenceStartDate = SentenceUtils.calculateSentenceStartDate(sentenceEntity)
      val firstSentenceAppearance = activeAppearances
        .minBy { it.appearanceDate }

      val latestRecall = sentenceEntity.latestRecall()
      val sentenceTypeAndCategory = getSentenceCalcTypeAndCategory(sentenceEntity, latestRecall)

      return LegacySentence(
        courtCase.prisonerId,
        courtCase.caseUniqueIdentifier,
        sentenceEntity.charge.chargeUuid,
        sentenceEntity.sentenceUuid,
        firstSentenceAppearance.appearanceUuid,
        sentenceEntity.statusId == SentenceEntityStatus.ACTIVE,
        sentenceTypeAndCategory.first,
        sentenceTypeAndCategory.second,
        sentenceEntity.consecutiveTo?.sentenceUuid,
        sentenceEntity.countNumber,
        sentenceEntity.fineAmount,
        sentenceStartDate,
        latestRecall?.returnToCustodyDate,
      )
    }

    fun getSentenceCalcTypeAndCategory(sentenceEntity: SentenceEntity, latestRecall: RecallEntity?): Pair<String, String> {
      return if (latestRecall == null) {
        (sentenceEntity.sentenceType?.nomisSentenceCalcType ?: sentenceEntity.legacyData!!.sentenceCalcType!!) to
          (sentenceEntity.sentenceType?.nomisCjaCode ?: sentenceEntity.legacyData!!.sentenceCategory!!)
      } else {
        if (LegacySentenceService.recallSentenceTypeBucketUuid == sentenceEntity.sentenceType?.sentenceTypeUuid) {
          val recallLegacyData = latestRecall.let { recall -> sentenceEntity.recallSentences.find { it.recall.id == recall.id }?.legacyData }
          if (recallLegacyData?.sentenceCalcType != null) {
            return recallLegacyData.sentenceCalcType to recallLegacyData.sentenceCategory!!
          } else {
            val earliestRecall = sentenceEntity.earliestRecall()!!
            val earliestRecallLegacyData = earliestRecall.let { recall -> sentenceEntity.recallSentences.find { it.recall.id == recall.id }?.legacyData }!!
            return mapNewRecallOnNomisRecall(latestRecall, earliestRecallLegacyData)
          }
        } else {
          latestRecall.recallType.toLegacySentenceType(sentenceEntity.sentenceType!!) to sentenceEntity.sentenceType!!.nomisCjaCode
        }
      }
    }

    private fun mapNewRecallOnNomisRecall(
      latestRecall: RecallEntity,
      recallLegacyData: RecallSentenceLegacyData,
    ): Pair<String, String> {
      val classification =
        LegacyRecallService.Companion.classificationToLegacySentenceTypeMap.mapNotNull { (classification, types) ->
          if (types.contains(recallLegacyData.sentenceCalcType)) {
            classification
          } else {
            null
          }
        }.firstOrNull()

      // Legacy recall can match more than one sentence type, but will all be standard sentences.
      if (classification == null && latestRecall.recallType.code == RecallType.LR && unknownPreRecallSentenceTypes.contains(recallLegacyData.sentenceCalcType)) {
        // A legacy recall has been recalled again in DPS and the sentence type has not been corrected
        throw IllegalStateException("Unknown legacy recall sentence type has been recalled again")
      }

      return latestRecall.recallType.toLegacySentenceType(recallLegacyData.sentenceCalcType!!, classification ?: SentenceTypeClassification.STANDARD) to recallLegacyData.sentenceCategory!!
    }
  }
}
