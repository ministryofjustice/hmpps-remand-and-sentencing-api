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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

interface CourtAppearanceRepository : CrudRepository<CourtAppearanceEntity, Int> {
  fun findByAppearanceUuid(appearanceUuid: UUID): CourtAppearanceEntity?

  @Query(
    value = """
    SELECT ca.* from
    court_appearance ca
    join court_case cc on cc.id=ca.court_case_id
    where cc.id= :courtCaseId and (ca.legacy_data->>'nextEventDateTime')::date = :nextEventDate
    order by ca.appearance_date desc
    limit 1
  """,
    nativeQuery = true,
  )
  fun findByNextEventDateTime(courtCaseId: Int, nextEventDate: LocalDate): CourtAppearanceEntity?

  fun findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(courtCaseEntity: CourtCaseEntity, status: CourtAppearanceEntityStatus): CourtAppearanceEntity?

  fun findByCourtCaseCaseUniqueIdentifierAndStatusId(courtCaseUuid: String, status: CourtAppearanceEntityStatus): CourtAppearanceEntity

  fun findAllByCourtCaseCaseUniqueIdentifierAndStatusId(courtCaseUuid: String, status: CourtAppearanceEntityStatus): List<CourtAppearanceEntity>

  fun findAllByCourtCasePrisonerIdAndStatusId(prisonerId: String, status: CourtAppearanceEntityStatus): List<CourtAppearanceEntity>

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
}
