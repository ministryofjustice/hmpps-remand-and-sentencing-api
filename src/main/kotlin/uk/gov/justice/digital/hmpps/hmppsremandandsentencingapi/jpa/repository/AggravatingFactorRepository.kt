package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AggravatingFactorEntity

interface AggravatingFactorRepository : CrudRepository<AggravatingFactorEntity, Int> {
  fun getAllByOrderByDisplayOrder(): List<AggravatingFactorEntity>
}
