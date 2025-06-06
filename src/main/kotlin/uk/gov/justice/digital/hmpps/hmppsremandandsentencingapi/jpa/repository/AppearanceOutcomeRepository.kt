package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

interface AppearanceOutcomeRepository : CrudRepository<AppearanceOutcomeEntity, Int> {
  fun findByOutcomeUuid(outcomeUuid: UUID): AppearanceOutcomeEntity?

  fun findByNomisCode(nomisCode: String): AppearanceOutcomeEntity?

  fun findByNomisCodeIn(nomisCodes: List<String>): List<AppearanceOutcomeEntity>

  fun findByStatusIn(statuses: List<ReferenceEntityStatus>): List<AppearanceOutcomeEntity>
}
