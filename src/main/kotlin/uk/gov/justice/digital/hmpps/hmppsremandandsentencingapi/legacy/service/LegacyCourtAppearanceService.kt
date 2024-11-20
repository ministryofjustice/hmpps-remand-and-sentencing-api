package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import kotlin.collections.plus

@Service
class LegacyCourtAppearanceService(private val courtAppearanceRepository: CourtAppearanceRepository, private val courtCaseRepository: CourtCaseRepository, private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun create(courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCaseUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${courtAppearance.courtCaseUuid}")
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(courtAppearance, dpsOutcome, courtCase, serviceUserService.getUsername()),
    )
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCase.appearances + createdCourtAppearance)
    return LegacyCourtAppearanceCreatedResponse(createdCourtAppearance.lifetimeUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }
}
