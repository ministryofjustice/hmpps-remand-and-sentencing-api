package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.util.UUID

data class Sentence(
  val sentenceUuid: UUID,
  val chargeNumber: String,
  val custodialPeriodLength: PeriodLength,
  val extendedLicensePeriodLength: PeriodLength?,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): Sentence {
      return Sentence(
        sentenceEntity.sentenceUuid,
        sentenceEntity.chargeNumber,
        PeriodLength.from(sentenceEntity.custodialPeriodLength),
        sentenceEntity.extendedLicensePeriodLength?.let { PeriodLength.from(it) },
      )
    }
  }
}
