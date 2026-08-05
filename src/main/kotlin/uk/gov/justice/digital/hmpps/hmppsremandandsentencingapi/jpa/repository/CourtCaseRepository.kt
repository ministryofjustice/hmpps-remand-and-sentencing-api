package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.PersonCourtCaseCount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.validate.CourtCaseValidationDate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom.CourtCaseSearchRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

interface CourtCaseRepository :
  CrudRepository<CourtCaseEntity, Int>,
  PagingAndSortingRepository<CourtCaseEntity, Int>,
  CourtCaseSearchRepository {

  @Query(
    """select count(*)
    from court_case cc
    join court_appearance lca on cc.latest_court_appearance_id = lca.id
    where cc.prisoner_id = :prisonerId and (cc.legacy_data->>'bookingId' = :bookingId or cc.legacy_data->>'bookingId' is null or :bookingId = '') 
    and lca.appearance_date >= :appearanceDateFrom
    and lca.appearance_date <= :appearanceDateTo
    and cc.status_id not in :courtCaseStatuses
  """,
    nativeQuery = true,
  )
  fun countCourtCasesForSearch(
    @Param("prisonerId") prisonerId: String,
    @Param("bookingId") bookingId: String,
    @Param("appearanceDateFrom") appearanceDateFrom: LocalDate,
    @Param("appearanceDateTo") appearanceDateTo: LocalDate,
    @Param("courtCaseStatuses") courtCaseStatus: List<String> = listOf(CourtCaseEntityStatus.DELETED.toString(), CourtCaseEntityStatus.DUPLICATE.toString()),
  ): Long

  @Query(
    """select count(cc)
    from CourtCaseEntity cc
    join cc.latestCourtAppearance lca
    where cc.prisonerId = :prisonerId 
    and cc.latestCourtAppearance is not null 
    and cc.statusId not in :courtCaseStatuses
  """,
  )
  fun countCourtCasesByPrisoner(
    @Param("prisonerId") prisonerId: String,
    @Param("courtCaseStatuses") courtCaseStatuses: List<CourtCaseEntityStatus> = listOf(CourtCaseEntityStatus.DELETED, CourtCaseEntityStatus.DUPLICATE),
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

  fun findAllByPrisonerIdAndStatusId(prisonerId: String, statusId: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE): List<CourtCaseEntity>

  fun findAllByPrisonerIdAndStatusIdNot(prisonerId: String, statusId: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED): List<CourtCaseEntity>

  fun findAllByPrisonerIdInAndStatusIdNot(prisonerIds: List<String>, statusId: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED): List<CourtCaseEntity>

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
    max(CASE WHEN (a.warrantType = 'NON_SENTENCING') THEN a.appearanceDate ELSE null END),
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
    select *, (select
            count(*) 
        from
            court_case cc1
        where
            cc1.merged_to_case_id= cc.id 
            and cc.status_id != 'DELETED') as totalMergedFromCount from court_case cc
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

  @Query(
    """
    select cc.caseUniqueIdentifier from CourtCaseEntity cc
    where cc.prisonerId = :prisonerId
    and cc.statusId != :statusId
  """,
  )
  fun findCaseUniqueIdentifierByPrisonerIdAndStatusIdNot(prisonerId: String, statusId: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED): List<String>

  @Modifying(clearAutomatically = true)
  @Query(
    """
    UPDATE court_case
    SET legacy_data['caseReferences'] = :caseReferences ::jsonb,
    updated_at = :updatedAt,
    updated_by = :updatedBy
    where id = :id
  """,
    nativeQuery = true,
  )
  fun updateLegacyDataCaseReferencesById(@Param("caseReferences")caseReferences: String, @Param("updatedAt") updatedAt: ZonedDateTime, @Param("updatedBy") updatedBy: String, @Param("id") id: Int)

  @Modifying(clearAutomatically = true)
  @Query(
    """
    UPDATE court_case
    SET legacy_data['bookingId'] = to_jsonb(:bookingId),
    status_id = :#{#statusId.name()},
    updated_at = :updatedAt,
    updated_by = :updatedBy
    where id = :id
  """,
    nativeQuery = true,
  )
  fun updateLegacyDataBookingIdById(@Param("bookingId")bookingId: Long?, @Param("statusId") statusId: CourtCaseEntityStatus, @Param("updatedAt") updatedAt: ZonedDateTime, @Param("updatedBy") updatedBy: String, @Param("id") id: Int)

  @Query(
    """
    select count(*) filter (where legacy_data->>'bookingId' = :bookingId or legacy_data->>'bookingId' is null) as suppliedBookingCount, 
    count(*) filter(where legacy_data->>'bookingId' != :bookingId and legacy_data->>'bookingId' is not null) as otherBookingCount 
    from court_case cc 
    where cc.prisoner_id = :prisonerId and cc.status_id != :statusId
  """,
    nativeQuery = true,
  )
  fun countCourtCasesForBooking(@Param("prisonerId") prisonerId: String, @Param("bookingId") bookingId: String, @Param("statusId") excludedStatusId: String = CourtCaseEntityStatus.DELETED.toString()): PersonCourtCaseCount

  @Query(
    """
    select cc from CourtCaseEntity cc
    join cc.appearances ca
    join ca.appearanceCharges ac
    join ac.charge c
    join c.sentences s
    left join s.periodLengths pl
    left join s.recallSentences rs
    where cc.statusId != :courtCaseStatus and 
    ca.statusId = :courtAppearanceStatus and 
    c.statusId = :chargeStatus and 
    s.statusId != :sentenceStatus and
    cc.caseUniqueIdentifier = :courtCaseUuid and
    rs is null
    order by cc.caseUniqueIdentifier desc limit 1
  """,
  )
  fun findSentencedCourtCase(
    @Param("courtCaseUuid") courtCaseUuid: String,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
    @Param("sentenceStatus") sentenceStatus: SentenceEntityStatus = SentenceEntityStatus.DELETED,
  ): CourtCaseEntity?

  @Query(
    value = """select cc.id from court_case cc
                join (
                  select ca.court_case_id, bool_or(s.consecutive_to_id is not null or consec.id is not null) as has_consecutive_to from sentence s 
                  left join sentence consec on consec.consecutive_to_id = s.id and consec.status_id != 'DELETED'
                  join charge c on c.id = s.charge_id
                  join appearance_charge ac on ac.charge_id = c.id
                  join court_appearance ca on ca.id = ac.appearance_id
                  where s.status_id = 'MANY_CHARGES_DATA_FIX'
                  group by ca.court_case_id) as consec_cases on consec_cases.court_case_id = cc.id
                order by (case when consec_cases.has_consecutive_to then 2 else 1 end) asc, cc.updated_at desc
                limit :limit
           """,
    nativeQuery = true,
  )
  fun findIdWithManyChargesDataFixByConsecutiveToLast(@Param("limit") limit: Int): Set<Int>

  @Query(
    """select count(cc)
    from CourtCaseEntity cc
    join cc.appearances app
    where cc.prisonerId = :prisonerId 
    and cc.latestCourtAppearance is not null 
    and cc.statusId not in :courtCaseStatuses
    and app.courtCaseReference = :courtCaseReference
  """,
  )
  fun countCourtCasesByPrisonerAndCourtCaseReference(
    @Param("prisonerId") prisonerId: String,
    @Param("courtCaseReference") courtCaseReference: String,
    @Param("courtCaseStatuses") courtCaseStatuses: List<CourtCaseEntityStatus> = listOf(CourtCaseEntityStatus.DELETED, CourtCaseEntityStatus.DUPLICATE),
  ): Long
}
