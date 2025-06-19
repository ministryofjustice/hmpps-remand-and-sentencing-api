package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.util.UUID

data class Sentence(
  val sentenceUuid: UUID,
  val chargeNumber: String?,
  val periodLengths: List<PeriodLength>,
  val sentenceServeType: String,
  val sentenceType: SentenceType?,
  val convictionDate: LocalDate?,
  val fineAmount: FineAmount?,
  val legacyData: SentenceLegacyData?,
  val consecutiveToSentenceUuid: UUID?,
  val hasRecall: Boolean,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence = Sentence(
      sentenceEntity.sentenceUuid,
      sentenceEntity.countNumber,
      sentenceEntity.periodLengths.filter { it.statusId == EntityStatus.ACTIVE }.map { PeriodLength.from(it) },
      sentenceEntity.sentenceServeType,
      sentenceEntity.sentenceType?.let { SentenceType.from(it) },
      sentenceEntity.convictionDate,
      sentenceEntity.fineAmount?.let { FineAmount(it) },
      sentenceEntity.legacyData,
      sentenceEntity.consecutiveTo?.takeUnless { it.statusId == EntityStatus.DELETED }?.sentenceUuid,
      sentenceEntity.totalRecallSentences > 0,
    )
  }
}
