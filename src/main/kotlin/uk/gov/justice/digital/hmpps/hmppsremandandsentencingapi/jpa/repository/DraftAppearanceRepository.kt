package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity
import java.util.UUID

interface DraftAppearanceRepository : CrudRepository<DraftAppearanceEntity, Int> {
  fun findByDraftUuid(draftUuid: UUID): DraftAppearanceEntity?
}
