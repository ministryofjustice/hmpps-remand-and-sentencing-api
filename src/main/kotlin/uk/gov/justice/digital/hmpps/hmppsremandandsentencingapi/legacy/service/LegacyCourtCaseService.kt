package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class LegacyCourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper) {

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

  @Transactional(readOnly = true)
  fun get(courtCaseUuid: String): LegacyCourtCase {
    val courtCase = getUnlessDeleted(courtCaseUuid)
    val courtCaseLegacyData = courtCase.legacyData?.let { legacyData -> objectMapper.treeToValue(legacyData, CourtCaseLegacyData::class.java) }
    return LegacyCourtCase.from(courtCase, courtCaseLegacyData)
  }

  @Transactional
  fun update(courtCaseUuid: String, courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    existingCourtCase.statusId = if (courtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE
    return LegacyCourtCaseCreatedResponse(existingCourtCase.caseUniqueIdentifier)
  }

  @Transactional
  fun delete(courtCaseUuid: String) {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    existingCourtCase.statusId = EntityStatus.DELETED
  }

  private fun getUnlessDeleted(courtCaseUuid: String): CourtCaseEntity {
    return courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)
      ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")
  }
}
