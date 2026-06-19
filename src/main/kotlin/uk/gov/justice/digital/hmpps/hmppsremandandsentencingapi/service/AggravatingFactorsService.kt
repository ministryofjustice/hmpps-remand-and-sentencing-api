package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeAggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AggravatingFactorRepository

@Service
class AggravatingFactorsService(
  private val aggravatingFactorRepository: AggravatingFactorRepository,
) {

  /**
   * Sets aggravating factors driven by the boolean
   * flags set on the charge. Eventually will be replaced
   * with a list of aggravating factors, but for now keeps
   * the chargeAggravatingFactors and charge boolean flags
   * in sync
   */
  fun replaceAggravatingFactors(
    charge: ChargeEntity,
  ) {
    val codes = listOfNotNull(
      AggravatingFactorCode.TERROR_CONNECTION.code.takeIf { charge.terrorRelated == true },
      AggravatingFactorCode.FOREIGN_POWER.code.takeIf { charge.foreignPowerRelated == true },
    ).toSet()

    charge.chargeAggravatingFactors.removeIf { existing ->
      existing.aggravatingFactor.code !in codes
    }

    val existingCodes = charge.chargeAggravatingFactors.map { it.aggravatingFactor.code }.toSet()
    val codesToAdd = codes - existingCodes

    if (codesToAdd.isEmpty()) {
      return
    }

    val missingAggravatingFactors = aggravatingFactorRepository.findByCodeIn(codesToAdd.toList())
    for (missingAggravatingFactor in missingAggravatingFactors) {
      charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, missingAggravatingFactor))
    }
  }

  enum class AggravatingFactorCode(val code: String) {
    TERROR_CONNECTION("OATC"),
    FOREIGN_POWER("OAFPC"),
  }
}
