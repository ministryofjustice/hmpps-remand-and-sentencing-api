package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.util.UUID

data class RecallableCourtCaseSentence(
  val sentenceUuid: UUID,
  val offenceCode: String?,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val outcome: String?,
  val sentenceType: String?,
  val classification: SentenceTypeClassification?,
  val systemOfRecord: String,
  val periodLengths: List<PeriodLength>,
  val convictionDate: LocalDate?,
  val chargeLegacyData: ChargeLegacyData?,
  val countNumber: String?,
  val lineNumber: String?,
  val sentenceServeType: String?,
  val sentenceLegacyData: SentenceLegacyData?,
  val outcomeDescription: String?,
  val isRecallable: Boolean,
  val sentenceTypeUuid: String,
  val sentenceDate: LocalDate?,
)
