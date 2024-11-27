package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.draft

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.DraftAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class DraftCourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val draftAppearanceRepository: DraftAppearanceRepository, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun create(draftCourtCase: DraftCreateCourtCase): DraftCourtCaseCreatedResponse {
    val createdByUsername = serviceUserService.getUsername()
    val draftCourtCaseEntity = courtCaseRepository.save(CourtCaseEntity.from(draftCourtCase, createdByUsername))
    val draftAppearances = draftCourtCase.draftAppearances.map { draftAppearance ->
      draftAppearanceRepository.save(DraftAppearanceEntity.from(draftAppearance, createdByUsername, draftCourtCaseEntity))
    }
    return DraftCourtCaseCreatedResponse.from(draftCourtCaseEntity, draftAppearances)
  }
}
