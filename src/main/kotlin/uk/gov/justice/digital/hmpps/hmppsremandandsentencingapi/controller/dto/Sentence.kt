package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.time.LocalDate
import java.util.UUID

data class Sentence(
  val sentenceUuid: UUID,
  val sentenceLifetimeUuid: UUID,
  val chargeNumber: String?,
  val periodLengths: List<PeriodLength>,
  val sentenceServeType: String,
  val consecutiveToChargeNumber: String?,
  val sentenceType: SentenceType?,
  val convictionDate: LocalDate?,
  val fineAmount: FineAmount?,
  val legacyData: JsonNode?,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence {
      return Sentence(
        sentenceEntity.sentenceUuid,
        sentenceEntity.lifetimeSentenceUuid,
        sentenceEntity.chargeNumber,
        sentenceEntity.periodLengths.map { PeriodLength.from(it) },
        sentenceEntity.sentenceServeType,
        sentenceEntity.consecutiveTo?.chargeNumber,
        sentenceEntity.sentenceType?.let { SentenceType.from(it) },
        sentenceEntity.convictionDate,
        sentenceEntity.fineAmountEntity?.let { FineAmount.from(it) },
        sentenceEntity.legacyData,
      )
    }
  }
}
