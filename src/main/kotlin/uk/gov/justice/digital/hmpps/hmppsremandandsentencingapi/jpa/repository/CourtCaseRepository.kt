package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom.CourtCaseSearchRepository
import java.time.LocalDate
import java.util.UUID

interface CourtCaseRepository :
  CrudRepository<CourtCaseEntity, Int>,
  PagingAndSortingRepository<CourtCaseEntity, Int>,
  CourtCaseSearchRepository {
  @EntityGraph(value = "CourtCaseEntity.withAppearancesAndOutcomes", type = EntityGraph.EntityGraphType.FETCH)
  fun findByPrisonerIdAndLatestCourtAppearanceIsNotNullAndStatusIdNot(
    prisonerId: String,
    statusId: EntityStatus = EntityStatus.DELETED,
    pageable: Pageable,
  ): Page<CourtCaseEntity>

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
    where cc.statusId in :#{#status} and 
    ca.statusId in :#{#status} and 
    c.statusId in :#{#status} and 
    s.statusId in :#{#sentenceStatuses} and
    cc.prisonerId = :prisonerId
  """,
  )
  fun findSentencedCourtCasesByPrisonerId(
    @Param("prisonerId") prisonerId: String,
    @Param("status") statuses: List<EntityStatus> = listOf(EntityStatus.ACTIVE),
    sentenceStatuses: List<EntityStatus> = statuses,
  ): List<CourtCaseEntity>

  fun findAllByPrisonerId(prisonerId: String): List<CourtCaseEntity>

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
  fun findSentenceCountNumbers(
    @Param("courtCaseUuid") courtCaseUuid: String,
    @Param("status") status: EntityStatus = EntityStatus.DELETED,
  ): List<String?>

  @Query(
    """
  select max(coalesce(c.offenceEndDate, c.offenceStartDate))
  from CourtCaseEntity cc
  join cc.appearances a
  join a.appearanceCharges ac
  join ac.charge c
  where cc.caseUniqueIdentifier = :uuid
    and a.statusId = :status
    and c.statusId = :status
  """,
  )
  fun findLatestOffenceDate(
    @Param("uuid") uuid: String,
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
  ): LocalDate?

  @Query(
    """
  select max(coalesce(c.offenceEndDate, c.offenceStartDate))
  from CourtCaseEntity cc
  join cc.appearances a
  join a.appearanceCharges ac
  join ac.charge c
  where cc.caseUniqueIdentifier = :uuid
    and a.statusId = :status
    and c.statusId = :status
    and a.appearanceUuid != :appearanceUuidToExclude
  """,
  )
  fun findLatestOffenceDateExcludingAppearance(
    @Param("uuid") uuid: String,
    @Param("appearanceUuidToExclude") appearanceUuidToExclude: UUID,
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
  ): LocalDate?

  @Query(
    """
    select s from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    where cc.caseUniqueIdentifier = :courtCaseUuid
    and cc.statusId != :status
    and ca.statusId != :status
    and c.statusId != :status
    and s.statusId != :status
  """,
  )
  fun findSentencesByCourtCaseUuid(
    @Param("courtCaseUuid") courtCaseUuid: String,
    @Param("status") status: EntityStatus = EntityStatus.DELETED,
  ): List<SentenceEntity>

  @Query(
    """
select cc from CourtCaseEntity cc
join cc.appearances a
where a.id = :courtAppearanceId
  and cc.statusId = :entityStatus
  and a.statusId = :entityStatus
""",
  )
  fun findByCourtAppearance(@Param("courtAppearanceId") courtAppearanceId: Int, @Param("entityStatus") entityStatus: EntityStatus = EntityStatus.ACTIVE): CourtCaseEntity?
}
