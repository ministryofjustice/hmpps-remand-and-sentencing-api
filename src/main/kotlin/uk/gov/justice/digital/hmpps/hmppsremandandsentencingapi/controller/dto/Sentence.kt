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
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence = Sentence(
      sentenceEntity.sentenceUuid,
      sentenceEntity.chargeNumber,
      sentenceEntity.periodLengths.filter { setOf(EntityStatus.ACTIVE, EntityStatus.MANY_CHARGES_DATA_FIX).contains(it.statusId) }.map { PeriodLength.from(it) },
      sentenceEntity.sentenceServeType,
      sentenceEntity.consecutiveTo?.chargeNumber,
      sentenceEntity.sentenceType?.let { SentenceType.from(it) },
      sentenceEntity.convictionDate,
      sentenceEntity.fineAmount?.let { FineAmount(it) },
      sentenceEntity.legacyData,
    )
  }
}
