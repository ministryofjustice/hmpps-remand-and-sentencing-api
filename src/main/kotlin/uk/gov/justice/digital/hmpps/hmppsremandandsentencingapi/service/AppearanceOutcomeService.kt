package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository

class AppearanceOutcomeService(private val appearanceOutcomeRepository: AppearanceOutcomeRepository) {

  fun getAll(): List<CourtAppearanceOutcome> = appearanceOutcomeRepository.findAll().map { CourtAppearanceOutcome.from(it) }
}
