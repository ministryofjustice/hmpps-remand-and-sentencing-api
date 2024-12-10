package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import java.util.UUID

interface AppearanceTypeRepository : CrudRepository<AppearanceTypeEntity, Int> {
  fun findByAppearanceTypeUuid(appearanceTypeUuid: UUID): AppearanceTypeEntity?
}
