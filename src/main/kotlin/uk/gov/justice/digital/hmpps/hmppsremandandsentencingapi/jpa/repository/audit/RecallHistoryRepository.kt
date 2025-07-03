package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import java.util.UUID

interface RecallHistoryRepository : CrudRepository<RecallHistoryEntity, Int> {
  fun findByRecallUuid(recallUuid: UUID): List<RecallHistoryEntity>
}
