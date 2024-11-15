package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class LegacyCourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun create(courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
    val createdCourtCase = courtCaseRepository.save(
      CourtCaseEntity.from(
        courtCase,
        serviceUserService.getUsername(),
      ),
    )
    return LegacyCourtCaseCreatedResponse(createdCourtCase.caseUniqueIdentifier)
  }
}
