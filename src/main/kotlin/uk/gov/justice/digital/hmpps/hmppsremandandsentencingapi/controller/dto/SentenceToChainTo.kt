package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class SentenceToChainTo(
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val sentenceUuid: UUID,
  val countNumber: String?,
  val chargeLegacyData: ChargeLegacyData?,
) {
  companion object {
    fun from(consecutiveToSentenceRow: ConsecutiveToSentenceRow): SentenceToChainTo = SentenceToChainTo(consecutiveToSentenceRow.offenceCode, consecutiveToSentenceRow.offenceStartDate, consecutiveToSentenceRow.offenceEndDate, consecutiveToSentenceRow.sentenceUuid, consecutiveToSentenceRow.countNumber, consecutiveToSentenceRow.chargeLegacyData)
  }
}
