package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.UpdatedCourtCaseReferences
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class CourtCaseReferenceService(private val courtCaseRepository: CourtCaseRepository, val courtAppearanceRepository: CourtAppearanceRepository, private val serviceUserService: ServiceUserService, private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository, private val courtCaseHistoryRepository: CourtCaseHistoryRepository) {

  @Transactional
  fun updateCourtCaseReferences(caseUniqueIdentifier: String): UpdatedCourtCaseReferences? = courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier)?.let { courtCaseEntity ->
    val appearanceStatuses = courtCaseEntity.appearances.groupBy { it.statusId == CourtAppearanceEntityStatus.ACTIVE }
    val activeCaseReferences = appearanceStatuses.getOrDefault(true, emptySet())
      .filter { courtAppearance -> courtAppearance.courtCaseReference != null }
      .map {
        CaseReferenceLegacyData(
          offenderCaseReference = it.courtCaseReference!!,
          updatedDate = it.createdAt.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime(),
          source = it.source,
        )
      }
    val inactiveCaseReferences = appearanceStatuses.getOrDefault(false, emptySet())
      .filter { courtAppearance -> courtAppearance.courtCaseReference != null }
      .filter { courtAppearance -> activeCaseReferences.none { it.offenderCaseReference == courtAppearance.courtCaseReference } }
      .map {
        CaseReferenceLegacyData(
          offenderCaseReference = it.courtCaseReference!!,
          updatedDate = it.createdAt.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime(),
          source = it.source,
        )
      }
    val existingCaseReferences = courtCaseEntity.legacyData?.caseReferences ?: mutableListOf()
    val toAddCaseReferences = activeCaseReferences
      .filter { activeCaseReference -> existingCaseReferences.none { existingCaseReference -> existingCaseReference.offenderCaseReference == activeCaseReference.offenderCaseReference } }
    existingCaseReferences.addAll(toAddCaseReferences)
    val toRemoveCaseReferences = inactiveCaseReferences.filter { inactiveCaseReference -> existingCaseReferences.any { existingCaseReference -> inactiveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }
    val activeCaseReferenceSet = activeCaseReferences.map { it.offenderCaseReference }.toSet()
    val dpsCaseReferencesToRemove = existingCaseReferences.filter { it.offenderCaseReference !in activeCaseReferenceSet && it.source == EventSource.DPS }
    val allCaseRefsToRemove = toRemoveCaseReferences.plus(dpsCaseReferencesToRemove)

    val toStoreCaseReferences = existingCaseReferences.filter { existingCaseReference -> allCaseRefsToRemove.none { toRemoveCaseReference -> toRemoveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }
    courtCaseEntity.legacyData = CourtCaseLegacyData(toStoreCaseReferences.toMutableList(), courtCaseEntity.legacyData?.bookingId)
    courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(courtCaseEntity))
    UpdatedCourtCaseReferences(courtCaseEntity.prisonerId, caseUniqueIdentifier, ZonedDateTime.now(), toAddCaseReferences.isNotEmpty() || toRemoveCaseReferences.isNotEmpty())
  }

  @Transactional
  fun refreshCaseReferences(courtCaseLegacyData: CourtCaseLegacyData, courtCaseUuid: String) {
    courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)?.let { courtCase ->
      courtCase.legacyData = courtCaseLegacyData
      courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(courtCase))
      val legacyCourtCaseReferences = courtCaseLegacyData.caseReferences.map { it.offenderCaseReference }.toSet()
      val toEditAppearances = courtCase.appearances.filter { it.statusId == CourtAppearanceEntityStatus.ACTIVE }.filter { it.courtCaseReference != null && !legacyCourtCaseReferences.contains(it.courtCaseReference) }
      toEditAppearances.forEach { editedAppearance ->
        editedAppearance.updatedAndRemoveCaseReference(serviceUserService.getUsername())
        courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(editedAppearance))
      }
    }
  }
}
