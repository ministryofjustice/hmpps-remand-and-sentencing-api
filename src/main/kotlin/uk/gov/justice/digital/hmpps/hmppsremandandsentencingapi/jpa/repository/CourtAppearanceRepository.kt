package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.linklatestappearance.LatestCourtAppearanceDataRow
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

interface CourtAppearanceRepository : CrudRepository<CourtAppearanceEntity, Int> {
  fun findByAppearanceUuid(appearanceUuid: UUID): CourtAppearanceEntity?

  @Query(
    value = """
    SELECT ca.* from
    court_appearance ca
    join court_case cc on cc.id=ca.court_case_id
    where cc.id= :courtCaseId and (ca.legacy_data->>'nextEventDateTime')::timestamp = :nextEventDateTime
    order by ca.appearance_date desc
    limit 1
  """,
    nativeQuery = true,
  )
  fun findByNextEventDateTime(courtCaseId: Int, nextEventDateTime: LocalDateTime): CourtAppearanceEntity?

  fun findFirstByCourtCaseAndStatusIdInAndIdNotOrderByAppearanceDateDesc(courtCaseEntity: CourtCaseEntity, statuses: List<CourtAppearanceEntityStatus>, id: Int): CourtAppearanceEntity?

  fun findByCourtCaseCaseUniqueIdentifierAndStatusId(courtCaseUuid: String, status: CourtAppearanceEntityStatus): CourtAppearanceEntity

  fun findAllByCourtCaseCaseUniqueIdentifierAndStatusId(courtCaseUuid: String, status: CourtAppearanceEntityStatus): List<CourtAppearanceEntity>

  fun findAllByCourtCasePrisonerIdAndStatusId(prisonerId: String, status: CourtAppearanceEntityStatus): List<CourtAppearanceEntity>

  fun findAllByCourtCasePrisonerIdAndStatusIdNot(prisonerId: String, status: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.DELETED): List<CourtAppearanceEntity>

  fun findAllByAppearanceUuidInAndStatusIdNot(appearanceUuids: List<UUID>, status: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.DELETED): List<CourtAppearanceEntity>

  @Modifying
  @Query(
    """
    DELETE FROM court_appearance
    WHERE court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteByCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying(clearAutomatically = true)
  @Query(
    """
    update CourtAppearanceEntity ca set ca.nextCourtAppearance = :nextCourtAppearance, ca.updatedBy = :updatedBy, ca.updatedAt = :updatedAt where ca = :courtAppearance
  """,
  )
  fun updateNextCourtAppearance(@Param("nextCourtAppearance") nextCourtAppearance: NextCourtAppearanceEntity, @Param("updatedBy") updatedBy: String, @Param("updatedAt") updatedAt: ZonedDateTime, @Param("courtAppearance") courtAppearanceEntity: CourtAppearanceEntity)

  @Query(
    """
      select ca, cc, ca.appearanceOutcome from CourtAppearanceEntity ca
      join ca.courtCase cc
      where
      ca.statusId = :courtAppearanceStatus and
      ca.source = "NOMIS" and
      cc.statusId in :courtCaseStatuses and
      cc.prisonerId = :prisonerId and
      ca.appearanceUuid not in :excludeAppearanceUuids
      order by ca.createdAt desc
    """,
  )
  fun findNomisImmigrationDetentionRecordsForPrisoner(
    @Param("prisonerId") prisonerId: String,
    @Param("excludeAppearanceUuids") excludeAppearanceUuids: List<UUID>,
    @Param("courtCaseStatuses") courtCaseStatus: List<CourtCaseEntityStatus> = listOf(CourtCaseEntityStatus.ACTIVE, CourtCaseEntityStatus.INACTIVE),
    @Param("courtAppearanceStatus") courtAppearanceStatuses: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE,
  ): List<CourtAppearanceEntity>

  @Modifying
  @Query(
    """
      update court_appearance set appearance_outcome_id = :appearanceOutcomeId, warrant_type= :warrantType where legacy_data->>'nomisOutcomeCode' = :nomisCode
    """,
    nativeQuery = true,
  )
  fun updateToSupportedAppearanceOutcome(@Param("appearanceOutcomeId") appearanceOutcomeId: Int, @Param("warrantType") warrantType: String, @Param("nomisCode") nomisCode: String)

  @Query(
    """
    select cc.id as court_case_id, ca.id as latest_appearance_id, fca.id as future_appearance_id, nca.id as next_court_appearance_id, to_remove_next_court_appearance.id as incorrect_appearance_id
    from court_case cc
    join court_appearance ca on cc.latest_court_appearance_id = ca.id
    join court_appearance fca on fca.court_case_id = cc.id and to_date(ca.legacy_data->>'nextEventDateTime', 'YYYY-MM-DD') = fca.appearance_date
    left join next_court_appearance nca on nca.future_skeleton_appearance_id = fca.id
    left join court_appearance to_remove_next_court_appearance on to_remove_next_court_appearance.next_court_appearance_id = nca.id
    where ca.next_court_appearance_id is null and ca.warrant_type = 'NON_SENTENCING' and ca.legacy_data->>'nextEventDateTime' is not null and fca.status_id = 'FUTURE'
  """,
    nativeQuery = true,
  )
  fun getLatestCourtAppearancesWithoutNextCourtAppearance(): List<LatestCourtAppearanceDataRow>

  fun findFirstByIdInOrderByCreatedAtAsc(ids: List<Int>): CourtAppearanceEntity
}
