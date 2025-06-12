package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity

interface AppearanceChargeHistoryRepository : CrudRepository<AppearanceChargeHistoryEntity, Int> {
  @Modifying
  @Query(
    """
  DELETE FROM AppearanceChargeHistoryEntity a 
  WHERE a.appearanceId IN :appearanceIds
""",
  )
  fun deleteAllByAppearanceIdIn(appearanceIds: List<Int>)
}
