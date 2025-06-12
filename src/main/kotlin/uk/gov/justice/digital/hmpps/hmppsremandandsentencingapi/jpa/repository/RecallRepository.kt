package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

interface RecallRepository : CrudRepository<RecallEntity, Int> {
  fun findOneByRecallUuid(recallUuid: UUID): RecallEntity?
  fun findByPrisonerIdAndStatusId(prisonerId: String, statusId: EntityStatus = EntityStatus.ACTIVE): List<RecallEntity>

  @Modifying
  @Query("DELETE FROM RecallEntity r WHERE r.prisonerId = :prisonerId")
  fun deleteAllByPrisonerId(prisonerId: String)
}
