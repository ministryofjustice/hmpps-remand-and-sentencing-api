package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PagedSentence(
  val chargeNumber: String?,
  val sentenceServeType: String,
  val consecutiveToSentenceUuid: UUID?,
  val convictionDate: LocalDate?,
  val sentenceTypeDescription: String?,
  val legacyData: SentenceLegacyData?,
  val fineAmount: BigDecimal?,
  val periodLengths: List<PagedSentencePeriodLength>,
) {
  companion object {
    fun from(sentenceRows: List<CourtCaseRow>): PagedSentence {
      val sentence = sentenceRows.first()
      val periodLengths = sentenceRows.filter { it.sentencePeriodLengthId != null && it.sentencePeriodLengthStatus != EntityStatus.DELETED }.groupBy { it.sentencePeriodLengthId }
      return PagedSentence(
        sentence.sentenceChargeNumber,
        sentence.sentenceServeType!!,
        sentence.sentenceConsecutiveToUuid,
        sentence.sentenceConvictionDate,
        sentence.sentenceTypeDescription ?: sentence.sentenceLegacyData?.sentenceTypeDesc,
        sentence.sentenceLegacyData,
        sentence.sentenceFineAmount,
        periodLengths.values.map { PagedSentencePeriodLength.from(it.first()) },
      )
    }
  }
}
