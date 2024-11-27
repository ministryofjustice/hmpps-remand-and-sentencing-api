package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.draft

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
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

  @Transactional
  fun createAppearanceInCourtCase(courtCaseUuid: String, draftCourtAppearance: DraftCreateCourtAppearance): DraftCourtAppearanceCreatedResponse {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid) ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")
    val draftAppearance = draftAppearanceRepository.save(DraftAppearanceEntity.from(draftCourtAppearance, serviceUserService.getUsername(), courtCase))
    return DraftCourtAppearanceCreatedResponse.from(draftAppearance)
  }
}
