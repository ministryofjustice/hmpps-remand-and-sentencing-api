package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.util.UUID

@Service
class CourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceService: CourtAppearanceService, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun createCourtCase(createCourtCase: CreateCourtCase): CourtCaseEntity {
    val courtCase = courtCaseRepository.save(CourtCaseEntity(prisonerId = createCourtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdByUsername = serviceUserService.getUsername(), statusId = EntityStatus.ACTIVE))
    courtCase.appearances = createCourtCase.appearances.map { courtAppearanceService.createCourtAppearance(it, courtCase) }
    courtCase.updateLatestCourtAppearance()

    return courtCaseRepository.save(courtCase)
  }

  @Transactional(readOnly = true)
  fun searchCourtCases(prisonerId: String, pageable: Pageable): Page<CourtCase> {
    return courtCaseRepository.findByPrisonerId(prisonerId, pageable).map {
      CourtCase.from(it)
    }
  }

  @Transactional(readOnly = true)
  fun getCourtCaseByUuid(courtCaseUUID: String): CourtCase? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let { CourtCase.from(it) }
}
