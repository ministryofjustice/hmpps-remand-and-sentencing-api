package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate

data class TestCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
  val startDate: LocalDate?,
  val courtId: String?,
  val caseReference: String?,
  val caseReferences: List<CaseReferenceLegacyData>,
  val appearances: List<LegacyCourtAppearance>,
) {
  companion object {
    val appearanceStatuses: Set<EntityStatus> = setOf(EntityStatus.ACTIVE, EntityStatus.FUTURE)
    fun from(courtCaseEntity: CourtCaseEntity, courtCaseLegacyData: CourtCaseLegacyData?, objectMapper: ObjectMapper): TestCourtCase {
      val firstAppearance = courtCaseEntity.appearances.firstOrNull { entity -> entity.statusId == EntityStatus.ACTIVE }
      return TestCourtCase(
        courtCaseEntity.caseUniqueIdentifier,
        courtCaseEntity.prisonerId,
        courtCaseEntity.statusId == EntityStatus.ACTIVE,
        firstAppearance?.appearanceDate,
        firstAppearance?.courtCode,
        firstAppearance?.courtCaseReference,
        courtCaseLegacyData?.caseReferences ?: emptyList(),
        courtCaseEntity.appearances.filter { appearanceStatuses.contains(it.statusId) }.map { LegacyCourtAppearance.from(it, objectMapper) },
      )
    }
  }
}
