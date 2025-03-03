package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import java.util.UUID

@Service
class AppearanceOutcomeService(private val appearanceOutcomeRepository: AppearanceOutcomeRepository) {

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<CourtAppearanceOutcome> = appearanceOutcomeRepository.findByStatusIn(statuses).map { CourtAppearanceOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): CourtAppearanceOutcome? = appearanceOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { CourtAppearanceOutcome.from(it) }
}
