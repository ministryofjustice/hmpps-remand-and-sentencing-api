package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.validate.CourtCaseValidationDate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom.CourtCaseSearchRepository
import java.time.LocalDate
import java.util.*

interface CourtCaseRepository :
  CrudRepository<CourtCaseEntity, Int>,
  PagingAndSortingRepository<CourtCaseEntity, Int>,
  CourtCaseSearchRepository {
  @EntityGraph(value = "CourtCaseEntity.withAppearancesAndOutcomes", type = EntityGraph.EntityGraphType.FETCH)
  fun findByPrisonerIdAndLatestCourtAppearanceIsNotNullAndStatusIdNot(
    prisonerId: String,
    statusId: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
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
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
  ): Long

  fun findByCaseUniqueIdentifier(caseUniqueIdentifier: String): CourtCaseEntity?

  @Query(
    """
    select cc from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    where cc.statusId = :courtCaseStatus and 
    ca.statusId in :courtAppearanceStatuses and 
    c.statusId = :chargeStatus and 
    s.statusId != :sentenceStatus and
    cc.prisonerId = :prisonerId
  """,
  )
  fun findSentencedCourtCasesByPrisonerId(
    @Param("prisonerId") prisonerId: String,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE,
    @Param("courtAppearanceStatuses") courtAppearanceStatuses: List<CourtAppearanceEntityStatus> = listOf(CourtAppearanceEntityStatus.ACTIVE, CourtAppearanceEntityStatus.RECALL_APPEARANCE),
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
    @Param("sentenceStatus") sentenceStatuses: SentenceEntityStatus = SentenceEntityStatus.DELETED,
  ): List<CourtCaseEntity>

  fun findAllByPrisonerId(prisonerId: String): List<CourtCaseEntity>

  @Query(
    """
    select s.countNumber from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    where cc.caseUniqueIdentifier = :courtCaseUuid and
    cc.statusId != :courtCaseStatus and 
    ca.statusId != :courtAppearanceStatus and 
    c.statusId != :chargeStatus and 
    s.statusId != :sentenceStatus
  """,
  )
  fun findSentenceCountNumbers(
    @Param("courtCaseUuid") courtCaseUuid: String,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.DELETED,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.DELETED,
    @Param("sentenceStatus") sentenceStatus: SentenceEntityStatus = SentenceEntityStatus.DELETED,
  ): List<String?>

  @Query(
    """
  select max(coalesce(c.offenceEndDate, c.offenceStartDate))
  from CourtCaseEntity cc
  join cc.appearances a
  join a.appearanceCharges ac
  join ac.charge c
  where cc.caseUniqueIdentifier = :uuid and
  cc.statusId = :courtCaseStatus and 
  a.statusId = :courtAppearanceStatus and 
  c.statusId = :chargeStatus
  """,
  )
  fun findLatestOffenceDate(
    @Param("uuid") uuid: String,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
  ): LocalDate?

  @Query(
    """
  select max(coalesce(c.offenceEndDate, c.offenceStartDate))
  from CourtCaseEntity cc
  join cc.appearances a
  join a.appearanceCharges ac
  join ac.charge c
  where cc.caseUniqueIdentifier = :uuid and 
  cc.statusId = :courtCaseStatus and 
  a.statusId = :courtAppearanceStatus and 
  c.statusId = :chargeStatus and 
  a.appearanceUuid != :appearanceUuidToExclude
  """,
  )
  fun findLatestOffenceDateExcludingAppearance(
    @Param("uuid") uuid: String,
    @Param("appearanceUuidToExclude") appearanceUuidToExclude: UUID,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
  ): LocalDate?

  @Query(
    """
  select new uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.validate.CourtCaseValidationDate(
    max(coalesce(c.offenceEndDate, c.offenceStartDate)),
    max(CASE WHEN (a.warrantType = 'REMAND') THEN a.appearanceDate ELSE null END),
    max(CASE WHEN (a.warrantType = 'SENTENCING') THEN a.appearanceDate ELSE null END)
  )
  from CourtCaseEntity cc
  join cc.appearances a
  join a.appearanceCharges ac
  join ac.charge c
  where cc.caseUniqueIdentifier = :uuid
    and cc.statusId = :courtCaseStatus
    and a.statusId = :courtAppearanceStatus
    and c.statusId = :chargeStatus
    and a.appearanceUuid != :appearanceUuidToExclude
  """,
  )
  fun findValidationDates(
    @Param("uuid") uuid: String,
    @Param("appearanceUuidToExclude") appearanceUuidToExclude: UUID,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
  ): CourtCaseValidationDate

  @Query(
    """
    select s from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    where cc.caseUniqueIdentifier = :courtCaseUuid and
    cc.statusId != :courtCaseStatus and 
    ca.statusId != :courtAppearanceStatus and 
    c.statusId != :chargeStatus and 
    s.statusId != :sentenceStatus
  """,
  )
  fun findSentencesByCourtCaseUuid(
    @Param("courtCaseUuid") courtCaseUuid: String,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.DELETED,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.DELETED,
    @Param("sentenceStatus") sentenceStatus: SentenceEntityStatus = SentenceEntityStatus.DELETED,
  ): List<SentenceEntity>

  @Query(
    value = """
    select * from court_case cc
    where cc.prisoner_id = :prisonerId
    and cc.legacy_data ->> 'bookingId' = :bookingId
  """,
    nativeQuery = true,
  )
  fun findByPrisonerIdAndBookingId(
    @Param("prisonerId") prisonerId: String,
    @Param("bookingId") bookingId: String,
  ): List<CourtCaseEntity>

  @Modifying
  @Query(
    """
    UPDATE court_case
    SET latest_court_appearance_id = NULL
    WHERE latest_court_appearance_id IN (
        SELECT a.id FROM court_appearance a
        JOIN court_case cc ON a.court_case_id = cc.id
        WHERE cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun updateLatestCourtAppearanceNullByPrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    DELETE FROM court_case WHERE prisoner_id = :prisonerId
  """,
    nativeQuery = true,
  )
  fun deleteByPrisonerId(@Param("prisonerId") prisonerId: String)
}
