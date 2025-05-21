package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus

interface CourtCaseRepository :
  CrudRepository<CourtCaseEntity, Int>,
  PagingAndSortingRepository<CourtCaseEntity, Int> {
  @EntityGraph(value = "CourtCaseEntity.withAppearancesAndOutcomes", type = EntityGraph.EntityGraphType.FETCH)
  fun findByPrisonerIdAndLatestCourtAppearanceIsNotNullAndStatusIdNot(prisonerId: String, statusId: EntityStatus = EntityStatus.DELETED, pageable: Pageable): Page<CourtCaseEntity>

  fun findByCaseUniqueIdentifier(caseUniqueIdentifier: String): CourtCaseEntity?

  @Query(
    """
    select cc from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    where cc.statusId = :#{#status} and 
    ca.statusId = :#{#status} and 
    c.statusId = :#{#status} and 
    s.statusId = :#{#status} and
    cc.prisonerId = :prisonerId
  """,
  )
  fun findSentencedCourtCasesByPrisonerId(@Param("prisonerId") prisonerId: String, @Param("status") status: EntityStatus = EntityStatus.ACTIVE): List<CourtCaseEntity>
}
