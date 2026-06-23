package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus

interface AggravatingFactorRepository : CrudRepository<AggravatingFactorEntity, Int> {
  fun findByStatusInOrderByDisplayOrder(status: List<AggravatingFactorStatus>): List<AggravatingFactorEntity>

  fun findByCodeIn(code: List<String>): List<AggravatingFactorEntity>
}
