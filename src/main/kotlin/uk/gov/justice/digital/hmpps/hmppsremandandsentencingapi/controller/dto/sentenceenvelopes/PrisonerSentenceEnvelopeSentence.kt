package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.MergedFromCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ViewSentenceRow
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PrisonerSentenceEnvelopeSentence(
  val sentenceUuid: UUID,
  val offenceCode: String,
  val offenceDescription: String?,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val offenceOutcome: String?,
  val countNumber: String?,
  val lineNumber: String?,
  val convictionDate: LocalDate?,
  val periodLengths: List<PrisonerSentenceEnvelopePeriodLength>,
  val sentenceServeType: String,
  val consecutiveToSentenceUuid: UUID?,
  val fineAmount: BigDecimal?,
  val courtCode: String,
  val caseReference: String?,
  val appearanceDate: LocalDate,
  val sentenceTypeDescription: String?,
  val mergedFromCase: MergedFromCase?,
  var orderInChain: Int,
) {
  companion object {
    fun from(viewSentenceRowEntry: Map.Entry<UUID, List<ViewSentenceRow>>): PrisonerSentenceEnvelopeSentence {
      val (sentenceUuid, viewSentenceRows) = viewSentenceRowEntry
      // all the same sentence UUID so can retrieve first row for many of the fields above
      val firstRow = viewSentenceRows.first()
      val periodLengths = viewSentenceRows.filter { it.periodLengthUuid != null }.groupBy { it.periodLengthUuid!! }

      return PrisonerSentenceEnvelopeSentence(
        sentenceUuid,
        firstRow.offenceCode,
        firstRow.chargeLegacyData?.offenceDescription,
        firstRow.offenceStartDate,
        firstRow.offenceEndDate,
        firstRow.dpsOffenceOutcome ?: firstRow.chargeLegacyData?.outcomeDescription,
        firstRow.countNumber,
        firstRow.sentenceLegacyData?.nomisLineReference,
        firstRow.convictionDate,
        periodLengths.map { PrisonerSentenceEnvelopePeriodLength.from(it) },
        firstRow.sentenceServeType,
        firstRow.consecutiveToSentenceUuid,
        firstRow.fineAmount,
        firstRow.courtCode,
        firstRow.courtCaseReference,
        firstRow.appearanceDate,
        firstRow.dpsSentenceType ?: firstRow.sentenceLegacyData?.sentenceTypeDesc,
        firstRow.mergedFromAppearanceId?.let { MergedFromCase.from(firstRow) },
        0,
      )
    }
  }
}
