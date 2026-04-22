package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity

data class SentenceTypeChargeOutcomes(
  val chargeOutcomes: List<ChargeOutcome>,
) {
  companion object {
    fun from(chargeOutcomeEntity: Set<ChargeOutcomeEntity>): SentenceTypeChargeOutcomes = SentenceTypeChargeOutcomes(chargeOutcomeEntity.map { ChargeOutcome.from(it) })
  }
}
