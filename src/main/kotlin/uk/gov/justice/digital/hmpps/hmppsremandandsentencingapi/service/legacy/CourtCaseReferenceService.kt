package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.UpdatedCourtCaseReferences
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class CourtCaseReferenceService(private val courtCaseRepository: CourtCaseRepository, val courtAppearanceRepository: CourtAppearanceRepository, private val serviceUserService: ServiceUserService, private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository) {

  @Transactional
  fun updateCourtCaseReferences(caseUniqueIdentifier: String): UpdatedCourtCaseReferences? = courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier)?.let { courtCaseEntity ->
    val appearanceStatuses = courtCaseEntity.appearances.groupBy { it.statusId == EntityStatus.ACTIVE }
    val activeCaseReferences = appearanceStatuses.getOrDefault(true, emptyList())
      .filter { courtAppearance -> courtAppearance.courtCaseReference != null }
      .map {
        CaseReferenceLegacyData(
          it.courtCaseReference!!,
          it.createdAt.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime(),
        )
      }
    val inactiveCaseReferences = appearanceStatuses.getOrDefault(false, emptyList())
      .filter { courtAppearance -> courtAppearance.courtCaseReference != null }
      .filter { courtAppearance -> activeCaseReferences.none { it.offenderCaseReference == courtAppearance.courtCaseReference } }
      .map {
        CaseReferenceLegacyData(
          it.courtCaseReference!!,
          it.createdAt.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime(),
        )
      }
    val existingCaseReferences = courtCaseEntity.legacyData?.caseReferences ?: mutableListOf()
    val toAddCaseReferences = activeCaseReferences
      .filter { activeCaseReference -> existingCaseReferences.none { existingCaseReference -> existingCaseReference.offenderCaseReference == activeCaseReference.offenderCaseReference } }
    existingCaseReferences.addAll(toAddCaseReferences)
    val toRemoveCaseReferences = inactiveCaseReferences.filter { inactiveCaseReference -> existingCaseReferences.any { existingCaseReference -> inactiveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }

    val toStoreCaseReferences = existingCaseReferences.filter { existingCaseReference -> toRemoveCaseReferences.none { toRemoveCaseReference -> toRemoveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }
    courtCaseEntity.legacyData = CourtCaseLegacyData(toStoreCaseReferences.toMutableList())
    UpdatedCourtCaseReferences(courtCaseEntity.prisonerId, caseUniqueIdentifier, ZonedDateTime.now(), toAddCaseReferences.isNotEmpty() || toRemoveCaseReferences.isNotEmpty())
  }

  @Transactional
  fun refreshCaseReferences(courtCaseLegacyData: CourtCaseLegacyData, courtCaseUuid: String) {
    courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)?.let { courtCase ->
      courtCase.legacyData = courtCaseLegacyData
      val legacyCourtCaseReferences = courtCaseLegacyData.caseReferences.map { it.offenderCaseReference }.toSet()
      val toEditAppearances = courtCase.appearances.filter { it.statusId == EntityStatus.ACTIVE }.filter { it.courtCaseReference != null && !legacyCourtCaseReferences.contains(it.courtCaseReference) }
      toEditAppearances.forEach { editedAppearance ->
        editedAppearance.updatedAndRemoveCaseReference(serviceUserService.getUsername())
        courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(editedAppearance))
      }
    }
  }
}
