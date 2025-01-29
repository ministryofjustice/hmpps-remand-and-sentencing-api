package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import java.util.UUID

interface CourtCaseRepository :
  CrudRepository<CourtCaseEntity, Int>,
  PagingAndSortingRepository<CourtCaseEntity, Int> {
  fun findByPrisonerId(prisonerId: String, pageable: Pageable): Page<CourtCaseEntity>

  fun findByCaseUniqueIdentifier(caseUniqueIdentifier: String): CourtCaseEntity?

  fun findFirstByAppearancesChargesChargeUuid(chargeUuid: UUID): CourtCaseEntity?
}
