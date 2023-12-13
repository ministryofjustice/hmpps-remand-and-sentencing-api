package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.util.UUID

@Service
class CourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceService: CourtAppearanceService, private val serviceUserService: ServiceUserService) {

  @Transactional
  suspend fun createCourtCase(createCourtCase: CreateCourtCase): CourtCaseEntity {
    val courtCase = courtCaseRepository.save(CourtCaseEntity(prisonerId = createCourtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), latestCourtAppearance = null, createdByUsername = serviceUserService.getUsername(), statusId = EntityStatus.ACTIVE))
    val appearances = createCourtCase.appearances.map { courtAppearanceService.createCourtAppearance(it, courtCase) }
    courtCase.latestCourtAppearance = appearances.maxBy { it.appearanceDate }
    return courtCaseRepository.save(courtCase)
  }
}
