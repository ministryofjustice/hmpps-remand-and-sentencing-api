package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.time.LocalDate
import java.util.UUID

data class SentenceToChainTo(
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val sentenceUuid: UUID,
  val countNumber: String?,
) {
  companion object {
    fun from(charge: ChargeEntity, sentence: SentenceEntity): SentenceToChainTo = SentenceToChainTo(charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate, sentence.sentenceUuid, sentence.chargeNumber)
  }
}
