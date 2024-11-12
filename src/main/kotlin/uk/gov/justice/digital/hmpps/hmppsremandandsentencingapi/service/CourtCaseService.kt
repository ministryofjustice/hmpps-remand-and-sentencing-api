package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtCaseException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.time.ZonedDateTime

@Service
class CourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceService: CourtAppearanceService, private val serviceUserService: ServiceUserService, private val snsService: SnsService, private val objectMapper: ObjectMapper) {

  @Transactional
  fun putCourtCase(createCourtCase: CreateCourtCase, caseUniqueIdentifier: String): CourtCaseEntity {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) ?: courtCaseRepository.save(CourtCaseEntity.placeholderEntity(createCourtCase.prisonerId, caseUniqueIdentifier, serviceUserService.getUsername(), createCourtCase.legacyData?.let { objectMapper.valueToTree<JsonNode>(it) }))

    if (createCourtCase.prisonerId != courtCase.prisonerId) {
      throw ImmutableCourtCaseException("Cannot change prisoner id in a court case")
    }
    return saveCourtCaseAppearances(courtCase, createCourtCase)
  }

  @Transactional
  fun createCourtCase(createCourtCase: CreateCourtCase): CourtCaseEntity {
    val courtCase = courtCaseRepository.save(CourtCaseEntity.placeholderEntity(prisonerId = createCourtCase.prisonerId, createdByUsername = serviceUserService.getUsername(), legacyData = createCourtCase.legacyData?.let { objectMapper.valueToTree<JsonNode>(it) }))
    return saveCourtCaseAppearances(courtCase, createCourtCase).also { savedCourtCase ->
      snsService.courtCaseInserted(savedCourtCase.prisonerId, savedCourtCase.caseUniqueIdentifier, savedCourtCase.createdAt)
    }
  }

  private fun saveCourtCaseAppearances(courtCase: CourtCaseEntity, createCourtCase: CreateCourtCase): CourtCaseEntity {
    val toDeleteAppearances = courtCase.appearances.filter { existingCourtAppearance -> createCourtCase.appearances.none { it.appearanceUuid == existingCourtAppearance.appearanceUuid } }
    toDeleteAppearances.forEach { courtAppearanceService.deleteCourtAppearance(it) }
    val appearances = createCourtCase.appearances.map { courtAppearanceService.createCourtAppearance(it, courtCase) }
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(appearances)
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

  @Transactional(readOnly = true)
  fun getLatestAppearanceByCourtCaseUuid(courtCaseUUID: String): CourtAppearance? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let { CourtAppearance.from(it.latestCourtAppearance!!) }

  @Transactional
  fun deleteCourtCase(courtCaseUuid: String) {
    courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)?.let { courtCaseEntity ->
      courtCaseEntity.statusId = EntityStatus.DELETED
      courtCaseEntity.appearances.filter { it.statusId == EntityStatus.ACTIVE }.forEach { courtAppearanceService.deleteCourtAppearance(it) }
      snsService.courtCaseDeleted(courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, ZonedDateTime.now())
    }
  }
}
