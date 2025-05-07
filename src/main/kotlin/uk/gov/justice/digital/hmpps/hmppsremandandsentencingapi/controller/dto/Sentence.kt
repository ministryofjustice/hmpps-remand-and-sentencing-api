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
  val consecutiveToChargeNumber: String?,
  val sentenceType: SentenceType?,
  val convictionDate: LocalDate?,
  val fineAmount: FineAmount?,
  val legacyData: SentenceLegacyData?,
  val sentenceId: Int?,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence = Sentence(
      sentenceEntity.sentenceUuid,
      sentenceEntity.chargeNumber,
      sentenceEntity.periodLengths.filter { it.statusId == EntityStatus.ACTIVE }.map { PeriodLength.from(it) },
      sentenceEntity.sentenceServeType,
      sentenceEntity.consecutiveTo?.chargeNumber,
      sentenceEntity.sentenceType?.let { SentenceType.from(it) },
      sentenceEntity.convictionDate,
      sentenceEntity.fineAmount?.let { FineAmount(it) },
      sentenceEntity.legacyData,
      sentenceEntity.id,
    )
  }
}
