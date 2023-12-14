package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity

interface CourtCaseRepository : CrudRepository<CourtCaseEntity, Int> {
  fun findByPrisonerId(prisonerId: String): List<CourtCaseEntity>
}
