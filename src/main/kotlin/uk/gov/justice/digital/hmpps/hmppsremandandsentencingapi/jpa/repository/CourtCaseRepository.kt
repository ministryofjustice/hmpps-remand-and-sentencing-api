package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom.CourtCaseSearchRepository

interface CourtCaseRepository :
  CrudRepository<CourtCaseEntity, Int>,
  PagingAndSortingRepository<CourtCaseEntity, Int>,
  CourtCaseSearchRepository {
  @EntityGraph(value = "CourtCaseEntity.withAppearancesAndOutcomes", type = EntityGraph.EntityGraphType.FETCH)
  fun findByPrisonerIdAndLatestCourtAppearanceIsNotNullAndStatusIdNot(prisonerId: String, statusId: EntityStatus = EntityStatus.DELETED, pageable: Pageable): Page<CourtCaseEntity>

  @Query(
    """select count(cc)
    from CourtCaseEntity cc
    join cc.latestCourtAppearance lca
    where cc.prisonerId = :prisonerId and cc.latestCourtAppearance is not null and cc.statusId != :courtCaseStatus
  """,
  )
  fun countCourtCases(
    @Param("prisonerId") prisonerId: String,
    @Param("courtCaseStatus") courtCaseStatus: EntityStatus = EntityStatus.DELETED,
  ): Long

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

  fun findAllByPrisonerId(prisonerId: String): List<CourtCaseEntity>

  @Modifying
  @Query("DELETE FROM CourtCaseEntity cc WHERE cc.id = :caseId")
  fun deleteAllByCourtCaseId(caseId: Int)

  @Query(
    """
    select s.countNumber from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    where s.statusId != :#{#status}
    and cc.caseUniqueIdentifier = :courtCaseUuid
    and c.statusId != :#{#status}
    and ca.statusId != :#{#status}
    and cc.statusId != :#{#status}
  """,
  )
  fun findSentenceCountNumbers(@Param("courtCaseUuid") courtCaseUuid: String, @Param("status") status: EntityStatus = EntityStatus.DELETED): List<String?>
}
