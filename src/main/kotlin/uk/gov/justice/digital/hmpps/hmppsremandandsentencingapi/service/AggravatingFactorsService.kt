package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

  fun getAllByStatuses(
    statuses: List<AggravatingFactorStatus>,
  ) = aggravatingFactorRepository
    .findByStatusInOrderByDisplayOrder(statuses)
    .map { AggravatingFactor.from(it) }

  /**
   * Scope both the add and remove ops to the current charge
   */
  fun replaceAggravatingFactors(
    charge: ChargeEntity,
    codes: Set<String>,
  ) {
    removeUnwantedChargeAggravatingFactors(charge, codes)

    val existingCodesOnThisCharge = charge.chargeAggravatingFactors
      .filter { it.charge == charge }
      .map { it.aggravatingFactor.code }
      .toSet()

    val codesToAdd = codes - existingCodesOnThisCharge

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
      val aggravatingFactorNotSelected =
        existing.aggravatingFactor.code !in codes

      val aggravatingFactorReferencingCharge =
        existing.charge == charge

      aggravatingFactorNotSelected && aggravatingFactorReferencingCharge
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
