package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ImmigrationDetentionHistoryEntity
import java.util.UUID

interface ImmigrationDetentionHistoryRepository : CrudRepository<ImmigrationDetentionHistoryEntity, Int> {
  fun findByImmigrationDetentionUuid(immigrationDetentionUuid: UUID): List<ImmigrationDetentionHistoryEntity>
}
