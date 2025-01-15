package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import java.util.UUID

interface RecallRepository : CrudRepository<RecallEntity, Int> {
  fun findOneByRecallUniqueIdentifier(recallUniqueIdentifier: UUID): RecallEntity?
  fun findByPrisonerId(prisonerId: String): List<RecallEntity>
  fun findFirstByPrisonerIdOrderByRecallDateDescCreatedAtDesc(prisonerId: String): RecallEntity?
}
