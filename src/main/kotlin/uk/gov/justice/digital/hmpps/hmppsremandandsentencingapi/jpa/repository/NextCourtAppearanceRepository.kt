package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.FirstNextCourtAppearance
import java.time.LocalDate

interface NextCourtAppearanceRepository : CrudRepository<NextCourtAppearanceEntity, Int> {
  fun findFirstByFutureSkeletonAppearance(futureSkeletonAppearance: CourtAppearanceEntity): NextCourtAppearanceEntity?

  fun findByFutureSkeletonAppearanceIdIn(ids: Set<Int>): List<NextCourtAppearanceEntity>

  @Modifying
  @Query(
    """
    delete from NextCourtAppearanceEntity nca
    where nca.futureSkeletonAppearance = :futureSkeletonAppearance
  """,
  )
  fun deleteByFutureSkeletonAppearance(@Param("futureSkeletonAppearance")futureSkeletonAppearance: CourtAppearanceEntity)

  @Modifying
  @Query(
    """
    DELETE FROM next_court_appearance
    WHERE future_skeleton_appearance_id IN (
    SELECT a.id FROM court_appearance a
    JOIN court_case cc ON a.court_case_id = cc.id
    WHERE cc.prisoner_id = :prisonerId
  )
  """,
    nativeQuery = true,
  )
  fun deleteByCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @EntityGraph("NextCourtAppearance.withAllAttributes")
  @Query(
    """
    select nca, lca.courtCaseReference from CourtAppearanceEntity ca
    join ca.nextCourtAppearance nca
    join ca.courtCase cc
    left join cc.latestCourtAppearance lca
    where cc.prisonerId = :prisonerId
    and cc.statusId != :courtCaseStatusId
    and ca.statusId != :courtAppearanceStatusId
    and nca.appearanceDate >= :from
    order by nca.appearanceDate asc, nca.appearanceTime asc
    limit 1
  """,
  )
  fun findFirstByPrisonerId(
    @Param("prisonerId") prisonerId: String,
    @Param("courtCaseStatusId") courtCaseStatusId: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
    @Param("courtAppearanceStatusId") courtAppearanceStatusId: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.DELETED,
    @Param("from") from: LocalDate = LocalDate.now(),
  ): FirstNextCourtAppearance?
}
