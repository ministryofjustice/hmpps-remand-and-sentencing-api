package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class RecallableSentence(
  val sentenceUuid: UUID,
  val offenceCode: String?,
  val sentenceType: String?,
  val classification: SentenceTypeClassification?,
  val systemOfRecord: String,
  val periodLengths: List<PeriodLength>,
  val convictionDate: LocalDate?,
  val chargeLegacyData: ChargeLegacyData?,
  val countNumber: String?,
  val sentenceServeType: String?,
)
