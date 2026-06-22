package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AggravatingFactorRepository

@Service
class AggravatingFactorService(private val aggravatingFactorRepository: AggravatingFactorRepository) {
  fun getAggravatingFactors() = aggravatingFactorRepository.getAllByOrderByDisplayOrder().map { AggravatingFactor.from(it) }
}
