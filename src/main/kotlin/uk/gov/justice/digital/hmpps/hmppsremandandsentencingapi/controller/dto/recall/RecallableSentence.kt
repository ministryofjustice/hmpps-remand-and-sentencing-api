package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.UUID

data class RecallableSentence(
  val sentenceId: UUID,
  val nomisSentenceId: NomisSentenceId?,
  val offenceCode: String?,
  val sentenceType: String?,
  val classification: SentenceTypeClassification?,
  val systemOfRecord: String,
  val startDate: LocalDate?,
  val periodLengths: List<PeriodLength>,
  val convictionDate: LocalDate?,
)

data class NomisSentenceId(
  val bookingId: Long,
  val sentenceSequence: Int,
)
