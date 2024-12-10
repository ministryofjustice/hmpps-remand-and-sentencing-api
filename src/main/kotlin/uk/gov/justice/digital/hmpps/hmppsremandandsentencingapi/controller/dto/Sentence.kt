package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.time.LocalDate
import java.util.UUID

data class Sentence(
  val sentenceUuid: UUID,
  val chargeNumber: String?,
  val periodLengths: List<PeriodLength>,
  val sentenceServeType: String,
  val consecutiveToChargeNumber: String?,
  val sentenceType: SentenceType,
  val convictionDate: LocalDate?,
  val fineAmount: FineAmount?,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence {
      return Sentence(
        sentenceEntity.sentenceUuid,
        sentenceEntity.chargeNumber,
        sentenceEntity.periodLengths.map { PeriodLength.from(it) },
        sentenceEntity.sentenceServeType,
        sentenceEntity.consecutiveTo?.chargeNumber,
        SentenceType.from(sentenceEntity.sentenceType),
        sentenceEntity.convictionDate,
        sentenceEntity.fineAmountEntity?.let { FineAmount.from(it) },
      )
    }
  }
}
