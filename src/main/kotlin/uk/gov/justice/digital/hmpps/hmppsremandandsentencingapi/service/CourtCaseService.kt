package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtCaseException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository

@Service
class CourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceService: CourtAppearanceService, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun putCourtCase(createCourtCase: CreateCourtCase, caseUniqueIdentifier: String): RecordResponse<CourtCaseEntity> {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) ?: courtCaseRepository.save(CourtCaseEntity.from(createCourtCase, serviceUserService.getUsername(), caseUniqueIdentifier))

    if (createCourtCase.prisonerId != courtCase.prisonerId) {
      throw ImmutableCourtCaseException("Cannot change prisoner id in a court case")
    }
    val (savedCourtCase, eventsToEmit) = saveCourtCaseAppearances(courtCase, createCourtCase)
    eventsToEmit.add(
      EventMetadataCreator.courtCaseEventMetadata(savedCourtCase.prisonerId, savedCourtCase.caseUniqueIdentifier, EventType.COURT_CASE_UPDATED),
    )
    return RecordResponse(
      savedCourtCase,
      eventsToEmit,
    )
  }

  @Transactional
  fun createCourtCase(createCourtCase: CreateCourtCase): RecordResponse<CourtCaseEntity> {
    val courtCase = courtCaseRepository.save(CourtCaseEntity.from(createCourtCase, serviceUserService.getUsername()))
    val (savedCourtCase, eventsToEmit) = saveCourtCaseAppearances(courtCase, createCourtCase)
    eventsToEmit.add(
      EventMetadataCreator.courtCaseEventMetadata(savedCourtCase.prisonerId, savedCourtCase.caseUniqueIdentifier, EventType.COURT_CASE_INSERTED),
    )
    return RecordResponse(
      savedCourtCase,
      eventsToEmit,
    )
  }

  private fun saveCourtCaseAppearances(courtCase: CourtCaseEntity, createCourtCase: CreateCourtCase): RecordResponse<CourtCaseEntity> {
    val toDeleteAppearances = courtCase.appearances.filter { existingCourtAppearance -> createCourtCase.appearances.none { it.appearanceUuid == existingCourtAppearance.appearanceUuid } }
    val eventsToEmit = toDeleteAppearances.flatMap { courtAppearanceService.deleteCourtAppearance(it).eventsToEmit }.toMutableSet()
    val appearanceRecords = createCourtCase.appearances.map { courtAppearanceService.createCourtAppearance(it, courtCase) }
    val appearances = appearanceRecords.map { it.record }
    eventsToEmit.addAll(appearanceRecords.flatMap { it.eventsToEmit })
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(appearances)
    return RecordResponse(courtCaseRepository.save(courtCase), eventsToEmit)
  }

  @Transactional(readOnly = true)
  fun searchCourtCases(prisonerId: String, pageable: Pageable): Page<CourtCase> = courtCaseRepository.findByPrisonerIdAndLatestCourtAppearanceIsNotNull(prisonerId, pageable).map {
    CourtCase.from(it)
  }

  @Transactional(readOnly = true)
  fun getCourtCaseByUuid(courtCaseUUID: String): CourtCase? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let { CourtCase.from(it) }

  @Transactional(readOnly = true)
  fun getLatestAppearanceByCourtCaseUuid(courtCaseUUID: String): CourtAppearance? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let { CourtAppearance.from(it.latestCourtAppearance!!) }
}
