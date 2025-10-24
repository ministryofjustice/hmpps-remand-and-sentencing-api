package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentence.details

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.FineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.util.UUID

data class SentenceDetails(
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
  val charge: SentenceDetailsCharge,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): SentenceDetails = SentenceDetails(
      sentenceEntity.sentenceUuid,
      sentenceEntity.countNumber,
      sentenceEntity.periodLengths.filter { it.statusId != PeriodLengthEntityStatus.DELETED }.map { PeriodLength.from(it) },
      sentenceEntity.sentenceServeType,
      sentenceEntity.sentenceType?.let { SentenceType.from(it) },
      sentenceEntity.convictionDate,
      sentenceEntity.fineAmount?.let { FineAmount(it) },
      sentenceEntity.legacyData,
      sentenceEntity.consecutiveTo?.takeUnless { it.statusId == SentenceEntityStatus.DELETED }?.sentenceUuid,
      sentenceEntity.totalRecallSentences > 0,
      SentenceDetailsCharge.from(sentenceEntity.charge),
    )
  }
}
