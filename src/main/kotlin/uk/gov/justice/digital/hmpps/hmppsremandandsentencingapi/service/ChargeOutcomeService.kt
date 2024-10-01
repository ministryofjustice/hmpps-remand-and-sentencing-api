package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import java.util.UUID

@Service
class ChargeOutcomeService(private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  fun getAll(): List<ChargeOutcome> = chargeOutcomeRepository.findAll().map { ChargeOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): ChargeOutcome? = chargeOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { ChargeOutcome.from(it) }
}
