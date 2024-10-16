package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.UpdatedCourtCaseReferences
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class CourtCaseReferenceService(private val courtCaseRepository: CourtCaseRepository, private val objectMapper: ObjectMapper) {

  @Transactional
  fun updateCourtCaseReferences(caseUniqueIdentifier: String): UpdatedCourtCaseReferences? =
    courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier)?.let { courtCaseEntity ->
      val appearanceStatuses = courtCaseEntity.appearances.groupBy { it.statusId == EntityStatus.ACTIVE }
      val activeCaseReferences = appearanceStatuses.getOrDefault(true, emptyList())
        .filter { courtAppearance -> courtAppearance.courtCaseReference != null }
        .map {
          CaseReferenceLegacyData(
            it.courtCaseReference!!,
            it.createdAt.format(
              DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            ),
          )
        }
      val inactiveCaseReferences = appearanceStatuses.getOrDefault(false, emptyList())
        .filter { courtAppearance -> courtAppearance.courtCaseReference != null }
        .filter { courtAppearance -> activeCaseReferences.none { it.offenderCaseReference == courtAppearance.courtCaseReference } }
        .map {
          CaseReferenceLegacyData(
            it.courtCaseReference!!,
            it.createdAt.format(
              DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            ),
          )
        }
      val existingCaseReferences = courtCaseEntity.legacyData?.let { objectMapper.treeToValue<CourtCaseLegacyData>(it, CourtCaseLegacyData::class.java).caseReferences } ?: mutableListOf()
      val toAddCaseReferences = activeCaseReferences
        .filter { activeCaseReference -> existingCaseReferences.none { existingCaseReference -> existingCaseReference.offenderCaseReference == activeCaseReference.offenderCaseReference } }
      existingCaseReferences.addAll(toAddCaseReferences)
      val toRemoveCaseReferences = inactiveCaseReferences.filter { inactiveCaseReference -> existingCaseReferences.any { existingCaseReference -> inactiveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }

      val toStoreCaseReferences = existingCaseReferences.filter { existingCaseReference -> toRemoveCaseReferences.none { toRemoveCaseReference -> toRemoveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }
      courtCaseEntity.legacyData = objectMapper.valueToTree<JsonNode>(CourtCaseLegacyData(toStoreCaseReferences.toMutableList()))
      UpdatedCourtCaseReferences(courtCaseEntity.prisonerId, caseUniqueIdentifier, ZonedDateTime.now(), toAddCaseReferences.isNotEmpty() || toRemoveCaseReferences.isNotEmpty())
    }
}
