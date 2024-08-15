package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.time.LocalDate
import java.util.UUID

data class Sentence(
  val sentenceUuid: UUID,
  val chargeNumber: String,
  val custodialPeriodLength: PeriodLength,
  val extendedLicensePeriodLength: PeriodLength?,
  val sentenceServeType: String,
  val consecutiveToChargeNumber: String?,
  val sentenceType: String,
  val convictionDate: LocalDate?,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence {
      return Sentence(
        sentenceEntity.sentenceUuid,
        sentenceEntity.chargeNumber,
        PeriodLength.from(sentenceEntity.custodialPeriodLength),
        sentenceEntity.extendedLicensePeriodLength?.let { PeriodLength.from(it) },
        sentenceEntity.sentenceServeType,
        sentenceEntity.consecutiveTo?.chargeNumber,
        sentenceEntity.sentenceType,
        sentenceEntity.convictionDate,
      )
    }
  }
}
