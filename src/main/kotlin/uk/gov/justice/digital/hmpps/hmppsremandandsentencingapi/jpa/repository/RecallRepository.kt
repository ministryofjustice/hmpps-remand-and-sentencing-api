package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import java.util.UUID

interface RecallRepository : CrudRepository<RecallEntity, Int> {
  fun findOneByRecallUuid(recallUuid: UUID): RecallEntity?
  fun findByPrisonerId(prisonerId: String): List<RecallEntity>
}
