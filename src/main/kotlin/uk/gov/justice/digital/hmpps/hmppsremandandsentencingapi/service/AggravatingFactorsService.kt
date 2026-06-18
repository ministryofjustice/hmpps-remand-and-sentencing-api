package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeAggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AggravatingFactorRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository

@Service
class AggravatingFactorsService(
  private val aggravatingFactorRepository: AggravatingFactorRepository,
  private val chargeRepository: ChargeRepository,
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
    )

    val existingCodes = charge.chargeAggravatingFactors.map { it.aggravatingFactor.code }.toSet()
    if (existingCodes == codes.toSet()) {
      return
    }

    charge.chargeAggravatingFactors.clear()
    chargeRepository.saveAndFlush(charge)

    val existingAggravatingFactors = aggravatingFactorRepository.findByCodeIn(codes)
    for (existingAggravatingFactor in existingAggravatingFactors) {
      charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, existingAggravatingFactor))
    }
  }

  enum class AggravatingFactorCode(val code: String) {
    TERROR_CONNECTION("OATC"),
    FOREIGN_POWER("OAFPC"),
  }
}
