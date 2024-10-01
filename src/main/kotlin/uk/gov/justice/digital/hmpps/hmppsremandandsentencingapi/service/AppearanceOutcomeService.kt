package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import java.util.UUID

@Service
class AppearanceOutcomeService(private val appearanceOutcomeRepository: AppearanceOutcomeRepository) {

  fun getAll(): List<CourtAppearanceOutcome> = appearanceOutcomeRepository.findAll().map { CourtAppearanceOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): CourtAppearanceOutcome? = appearanceOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { CourtAppearanceOutcome.from(it) }
}
