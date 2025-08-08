package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import java.time.LocalDate
import java.util.UUID

data class SentenceConsecutiveToDetails(
  val courtCaseReference: String?,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val sentenceUuid: UUID,
  val countNumber: String?,
) {
  companion object {
    fun from(consecutiveToSentenceRow: ConsecutiveToSentenceRow): SentenceConsecutiveToDetails = SentenceConsecutiveToDetails(
      consecutiveToSentenceRow.appearanceCourtCaseReference,
      consecutiveToSentenceRow.appearanceCourtCode,
      consecutiveToSentenceRow.appearanceDate,
      consecutiveToSentenceRow.offenceCode,
      consecutiveToSentenceRow.offenceStartDate,
      consecutiveToSentenceRow.offenceEndDate,
      consecutiveToSentenceRow.sentenceUuid,
      consecutiveToSentenceRow.countNumber,
    )
  }
}
