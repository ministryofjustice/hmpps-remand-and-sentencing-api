package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.charges

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.FineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.util.*

data class PersonSentence(
  val sentenceUuid: UUID,
  val chargeNumber: String?,
  val sentenceServeType: String,
  val sentenceType: SentenceType?,
  val convictionDate: LocalDate?,
  val fineAmount: FineAmount?,
  val legacyData: SentenceLegacyData?,
  val consecutiveToSentenceUuid: UUID?,
  val hasRecall: Boolean,
  val status: SentenceEntityStatus,
  val periodLengths: List<PersonPeriodLength>,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): PersonSentence = PersonSentence(
      sentenceEntity.sentenceUuid,
      sentenceEntity.countNumber,
      sentenceEntity.sentenceServeType,
      sentenceEntity.sentenceType?.let { SentenceType.from(it) },
      sentenceEntity.convictionDate,
      sentenceEntity.fineAmount?.let { FineAmount(it) },
      sentenceEntity.legacyData,
      sentenceEntity.consecutiveTo?.takeUnless { it.statusId == SentenceEntityStatus.DELETED }?.sentenceUuid,
      sentenceEntity.totalRecallSentences > 0,
      sentenceEntity.statusId,
      sentenceEntity.periodLengths.filter { it.statusId != PeriodLengthEntityStatus.DELETED }.map { PersonPeriodLength.from(it) },
    )
  }
}
