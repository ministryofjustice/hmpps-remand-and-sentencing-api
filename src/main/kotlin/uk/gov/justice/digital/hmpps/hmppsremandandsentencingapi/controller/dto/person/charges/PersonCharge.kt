package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.charges

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class PersonCharge(
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val outcome: ChargeOutcome?,
  val aggravatingFactors: List<AggravatingFactor>,
  val legacyData: ChargeLegacyData?,
  val findingOfDomesticAbuse: Boolean?,
  val sentence: PersonSentence?,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): PersonCharge = PersonCharge(
      chargeEntity.chargeUuid,
      chargeEntity.offenceCode,
      chargeEntity.offenceStartDate,
      chargeEntity.offenceEndDate,
      chargeEntity.chargeOutcome?.let { ChargeOutcome.from(it) },
      chargeEntity.chargeAggravatingFactors.map { AggravatingFactor.from(it.aggravatingFactor) },
      chargeEntity.legacyData,
      chargeEntity.findingOfDomesticAbuse,
      chargeEntity.getLiveSentence()?.let { PersonSentence.from(it) },
    )
  }
}
