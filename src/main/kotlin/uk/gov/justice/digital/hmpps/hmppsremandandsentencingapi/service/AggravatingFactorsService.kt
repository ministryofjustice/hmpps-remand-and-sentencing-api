package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeAggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AggravatingFactorRepository

@Service
class AggravatingFactorsService(
  private val aggravatingFactorRepository: AggravatingFactorRepository,
) {
  fun getAllByStatuses(statuses: List<AggravatingFactorStatus>) = aggravatingFactorRepository.findByStatusInOrderByDisplayOrder(statuses).map { AggravatingFactor.from(it) }

  /**
   * Sets aggravating factors driven by the set of codes passed in.
   * Eventually the boolean flags set directly on the charge will
   * be removed but until then ensure they are in sync with the
   * set of codes parameter
   */
  fun replaceAggravatingFactors(
    charge: ChargeEntity,
    codes: Set<String>,
  ) {
    val validCodes = checkChargesFlagsAndCodesSetInSync(codes, charge)
    removeUnwantedChargeAggravatingFactors(charge, validCodes)

    val existingCodes = charge.chargeAggravatingFactors.map { it.aggravatingFactor.code }.toSet()
    val codesToAdd = validCodes - existingCodes
    if (codesToAdd.isEmpty()) {
      return
    }

    val missingAggravatingFactors = aggravatingFactorRepository.findByCodeIn(codesToAdd.toList())
    for (missingAggravatingFactor in missingAggravatingFactors) {
      charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, missingAggravatingFactor))
    }
  }

  private fun removeUnwantedChargeAggravatingFactors(
    charge: ChargeEntity,
    codes: Set<String>,
  ) {
    charge.chargeAggravatingFactors.removeIf { existing ->
      val aggravatingFactorNotSetAsChargeFlag = existing.aggravatingFactor.code !in codes
      val aggravatingFactorReferencingPreviousCharge = existing.charge != charge
      aggravatingFactorNotSetAsChargeFlag || aggravatingFactorReferencingPreviousCharge
    }
  }

  /**
   * This check to be removed once boolean flags on charges
   * are ready for removal
   */
  private fun checkChargesFlagsAndCodesSetInSync(
    codes: Set<String>,
    charge: ChargeEntity,
  ): Set<String> {
    val codesSetOnChargeFlags = listOfNotNull(
      AggravatingFactorCode.TERROR_CONNECTION.code.takeIf { charge.terrorRelated == true },
      AggravatingFactorCode.FOREIGN_POWER.code.takeIf { charge.foreignPowerRelated == true },
    ).toSet()

    if (codes.isEmpty()) {
      return codesSetOnChargeFlags
    }

    val knownCodes = AggravatingFactorCode.entries.map { it.code }.toSet()
    val codesInListForKnownAggravatingFactors = codes.filter { it in knownCodes }.toSet()
    if (codesSetOnChargeFlags != codesInListForKnownAggravatingFactors) {
      throw RuntimeException("ChargeId ${charge.id}. Aggravating factors list mis-match aggravating factors set on charge")
    }

    return codes
  }

  enum class AggravatingFactorCode(val code: String) {
    TERROR_CONNECTION("OATC"),
    FOREIGN_POWER("OAFPC"),
  }
}
