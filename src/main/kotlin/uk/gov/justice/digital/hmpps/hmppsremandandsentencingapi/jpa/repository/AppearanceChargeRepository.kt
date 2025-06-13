package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable.AppearanceChargeId

interface AppearanceChargeRepository : CrudRepository<AppearanceChargeEntity, AppearanceChargeId> {
  @Modifying
  @Query(
    """
    DELETE FROM AppearanceChargeEntity ac 
    WHERE ac.appearance.courtCase.id = :caseId
  """,
  )
  fun deleteAllByAppearanceCourtCaseId(caseId: Int)
}
