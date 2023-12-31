package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity

interface AppearanceOutcomeRepository : CrudRepository<AppearanceOutcomeEntity, Int> {
  fun findByOutcomeName(outcomeName: String): AppearanceOutcomeEntity?
}
