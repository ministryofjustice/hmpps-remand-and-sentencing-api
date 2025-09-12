package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.*

interface RecallRepository : CrudRepository<RecallEntity, Int> {
  fun findOneByRecallUuid(recallUuid: UUID): RecallEntity?
  fun findByPrisonerIdAndStatusId(prisonerId: String, statusId: EntityStatus = EntityStatus.ACTIVE): List<RecallEntity>
  fun findByPrisonerId(prisonerId: String): List<RecallEntity>
}
