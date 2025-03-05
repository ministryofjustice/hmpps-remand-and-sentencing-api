package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.util.UUID

interface SentenceRepository : CrudRepository<SentenceEntity, Int> {
  fun findBySentenceUuid(sentenceUuid: UUID): SentenceEntity?

  fun findBySentenceUuidIn(sentenceUuids: List<UUID>): List<SentenceEntity>
}
