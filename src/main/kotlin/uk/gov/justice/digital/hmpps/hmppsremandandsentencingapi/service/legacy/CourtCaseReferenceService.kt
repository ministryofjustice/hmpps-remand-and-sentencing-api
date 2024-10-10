package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.time.format.DateTimeFormatter

@Service
class CourtCaseReferenceService(private val courtCaseRepository: CourtCaseRepository, private val objectMapper: ObjectMapper) {

  @Transactional
  fun updateCourtCaseReferences(caseUniqueIdentifier: String) {
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
      existingCaseReferences.addAll(
        activeCaseReferences
          .filter { activeCaseReference -> existingCaseReferences.none { existingCaseReference -> existingCaseReference.offenderCaseReference == activeCaseReference.offenderCaseReference } },
      )
      val toStoreCaseReferences = existingCaseReferences.filter { existingCaseReference -> inactiveCaseReferences.none { inactiveCaseReference -> inactiveCaseReference.offenderCaseReference == existingCaseReference.offenderCaseReference } }
      courtCaseEntity.legacyData = objectMapper.valueToTree<JsonNode>(CourtCaseLegacyData(toStoreCaseReferences.toMutableList()))
    }
  }
}
