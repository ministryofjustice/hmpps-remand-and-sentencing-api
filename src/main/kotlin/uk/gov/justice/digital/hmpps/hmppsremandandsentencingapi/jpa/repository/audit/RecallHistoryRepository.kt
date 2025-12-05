package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import java.util.UUID

interface RecallHistoryRepository : CrudRepository<RecallHistoryEntity, Int> {
  fun findByRecallUuid(recallUuid: UUID): List<RecallHistoryEntity>

  @Modifying
  @Query(
    """
    DELETE FROM recall_history rh WHERE rh.prisoner_id = :prisonerId
  """,
    nativeQuery = true,
  )
  fun deleteByPrisonerId(@Param("prisonerId") prisonerId: String)
}
