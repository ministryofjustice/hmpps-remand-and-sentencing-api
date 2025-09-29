package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PrisonerSentenceEnvelopeSentence(
  val sentenceUuiD: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val offenceOutcome: String,
  val countNumber: String?,
  val lineNumber: String?,
  val convictionDate: LocalDate?,
  val periodLengths: List<PrisonerSentenceEnvelopePeriodLength>,
  val sentenceServeType: String,
  val consecutiveToSentenceUuid: UUID,
  val fineAmount: BigDecimal?,
)
