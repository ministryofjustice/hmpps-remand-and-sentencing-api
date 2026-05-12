package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus

data class SentencedCharges(
  val charges: List<Charge>,
) {
  companion object {
    fun from(chargeEntities: List<ChargeEntity>): SentencedCharges = SentencedCharges(
      chargeEntities.filter { it.sentences.any { sentence -> sentence.statusId == SentenceEntityStatus.ACTIVE } }.map {
        Charge.from(it, { charge ->
          charge.sentences.firstOrNull {
            it.statusId ==
              SentenceEntityStatus.ACTIVE
          }
        })
      },
    )
  }
}
