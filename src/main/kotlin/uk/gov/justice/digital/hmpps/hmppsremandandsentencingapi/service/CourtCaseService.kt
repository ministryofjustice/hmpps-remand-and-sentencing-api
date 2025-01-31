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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetaData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtCaseException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.DraftAppearanceRepository

@Service
class CourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceService: CourtAppearanceService, private val serviceUserService: ServiceUserService, private val courtCaseDomainEventService: CourtCaseDomainEventService, private val objectMapper: ObjectMapper, private val draftAppearanceRepository: DraftAppearanceRepository) {

  @Transactional
  fun putCourtCase(createCourtCase: CreateCourtCase, caseUniqueIdentifier: String): RecordResponse<CourtCaseEntity> {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) ?: courtCaseRepository.save(CourtCaseEntity.placeholderEntity(createCourtCase.prisonerId, caseUniqueIdentifier, serviceUserService.getUsername(), createCourtCase.legacyData?.let { objectMapper.valueToTree<JsonNode>(it) }))

    if (createCourtCase.prisonerId != courtCase.prisonerId) {
      throw ImmutableCourtCaseException("Cannot change prisoner id in a court case")
    }
    val savedCourtCase = saveCourtCaseAppearances(courtCase, createCourtCase)
    return RecordResponse(
      savedCourtCase,
      mutableListOf(
        EventMetaData(
          savedCourtCase.prisonerId,
          savedCourtCase.caseUniqueIdentifier,
          EventType.COURT_CASE_UPDATED,
        ),
      ),
    )
  }

  @Transactional
  fun createCourtCase(createCourtCase: CreateCourtCase): RecordResponse<CourtCaseEntity> {
    val courtCase = courtCaseRepository.save(CourtCaseEntity.placeholderEntity(prisonerId = createCourtCase.prisonerId, createdByUsername = serviceUserService.getUsername(), legacyData = createCourtCase.legacyData?.let { objectMapper.valueToTree<JsonNode>(it) }))
    val savedCourtCase = saveCourtCaseAppearances(courtCase, createCourtCase)
    return RecordResponse(
      savedCourtCase,
      mutableListOf(
        EventMetaData(
          savedCourtCase.prisonerId,
          savedCourtCase.caseUniqueIdentifier,
          EventType.COURT_CASE_INSERTED,
        ),
      ),
    )
  }

  private fun saveCourtCaseAppearances(courtCase: CourtCaseEntity, createCourtCase: CreateCourtCase): CourtCaseEntity {
    val toDeleteAppearances = courtCase.appearances.filter { existingCourtAppearance -> createCourtCase.appearances.none { it.appearanceUuid == existingCourtAppearance.appearanceUuid } }
    toDeleteAppearances.forEach { courtAppearanceService.deleteCourtAppearance(it) }
    val appearances = createCourtCase.appearances.map { courtAppearanceService.createCourtAppearance(it, courtCase) }
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(appearances)
    return courtCaseRepository.save(courtCase)
  }

  @Transactional(readOnly = true)
  fun searchCourtCases(prisonerId: String, pageable: Pageable): Page<CourtCase> = courtCaseRepository.findByPrisonerId(prisonerId, pageable).map {
    CourtCase.from(it)
  }

  @Transactional(readOnly = true)
  fun getCourtCaseByUuid(courtCaseUUID: String): CourtCase? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let { CourtCase.from(it) }

  @Transactional(readOnly = true)
  fun getLatestAppearanceByCourtCaseUuid(courtCaseUUID: String): CourtAppearance? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let { CourtAppearance.from(it.latestCourtAppearance!!) }
}
