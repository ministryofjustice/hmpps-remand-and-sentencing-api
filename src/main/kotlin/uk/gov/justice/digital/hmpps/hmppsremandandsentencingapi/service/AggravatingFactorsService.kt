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

  fun getAllByStatuses(
    statuses: List<AggravatingFactorStatus>,
  ) = aggravatingFactorRepository
    .findByStatusInOrderByDisplayOrder(statuses)
    .map { AggravatingFactor.from(it) }

  fun replaceAggravatingFactors(
    charge: ChargeEntity,
    codes: Set<String>,
  ) {
    removeUnwantedChargeAggravatingFactors(charge, codes)

    val existingCodes = charge.chargeAggravatingFactors
      .map { it.aggravatingFactor.code }
      .toSet()

    val codesToAdd = codes - existingCodes

    if (codesToAdd.isEmpty()) {
      return
    }

    aggravatingFactorRepository
      .findByCodeIn(codesToAdd.toList())
      .forEach { aggravatingFactor ->
        charge.chargeAggravatingFactors.add(
          ChargeAggravatingFactorEntity(
            charge,
            aggravatingFactor,
          ),
        )
      }
  }

  private fun removeUnwantedChargeAggravatingFactors(
    charge: ChargeEntity,
    codes: Set<String>,
  ) {
    charge.chargeAggravatingFactors.removeIf { existing ->
      val aggravatingFactorNotSelected =
        existing.aggravatingFactor.code !in codes

      val aggravatingFactorReferencingPreviousCharge =
        existing.charge != charge

      aggravatingFactorNotSelected ||
        aggravatingFactorReferencingPreviousCharge
    }
  }
}
