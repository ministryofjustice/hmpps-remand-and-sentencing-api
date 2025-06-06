package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import java.util.UUID

@Service
class ChargeOutcomeService(private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<ChargeOutcome> = chargeOutcomeRepository.findByStatusIn(statuses).map { ChargeOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): ChargeOutcome? = chargeOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { ChargeOutcome.from(it) }

  fun findByUuids(outcomeUuids: List<UUID>): List<ChargeOutcome> = chargeOutcomeRepository.findByOutcomeUuidIn(outcomeUuids).map { ChargeOutcome.from(it) }
}
