package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.MissingSentenceInformationDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.SentenceAfterOnAnotherCourtAppearanceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ViewSentenceRow
import java.time.LocalDate
import java.util.*

interface SentenceRepository : CrudRepository<SentenceEntity, Int> {
  fun findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid: UUID, status: SentenceEntityStatus = SentenceEntityStatus.DELETED): SentenceEntity?

  fun findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(sentenceUuid: UUID, chargeUUID: UUID): SentenceEntity?

  fun findBySentenceUuidAndChargeChargeUuidNotInAndStatusIdNot(sentenceUuid: UUID, chargeUuids: List<UUID>, statusId: SentenceEntityStatus = SentenceEntityStatus.DELETED): List<SentenceEntity>

  @Query(
    """
    select s from SentenceEntity s
      join s.charge c
      join c.appearanceCharges ac
      join ac.appearance ca 
      where s.sentenceUuid = :sentenceUuid
      and c.chargeUuid in :chargeUuids
      and ca.appearanceUuid != :appearanceUuid
      and s.statusId != :statusId
  """,
  )
  fun findBySentenceUuidAndChargeUuidsAndNotAppearanceUuidAndStatusIdNot(sentenceUuid: UUID, chargeUuids: List<UUID>, appearanceUuid: UUID, statusId: SentenceEntityStatus = SentenceEntityStatus.DELETED): List<SentenceEntity>

  fun findBySentenceUuidIn(sentenceUuids: List<UUID>): List<SentenceEntity>

  fun findBySentenceUuid(sentenceUuid: UUID): List<SentenceEntity>

  fun findBySentenceUuidAndStatusId(sentenceUuid: UUID, status: SentenceEntityStatus): List<SentenceEntity>

  @Query(
    value = """
      select
      ca.appearance_uuid as appearanceUuid,
      ca.appearance_date as appearanceDate,
      ca.court_code as courtCode,
      ca.court_case_reference as courtCaseReference,
      s.sentence_uuid as sentenceUuid,
      c.offence_code as offenceCode,
      c.offence_start_date as offenceStartDate,
      s.count_number as countNumber,
      s.conviction_date as convictionDate,
      s.sentence_serve_type as sentenceServeType,
      st.id as sentenceTypeId,
      st.description as sentenceTypeDescription,
      pl.period_length_uuid as periodLengthUuid,
      pl.years as years,
      pl.months as months,
      pl.weeks as weeks,
      pl.days as days,
      pl.period_order as periodOrder,
      pl.period_length_type as periodLengthType
    from sentence s
    join sentence_type st on s.sentence_type_id = st.id
    join charge c on s.charge_id = c.id
    join appearance_charge ac on c.id = ac.charge_id
    join court_appearance ca on ac.appearance_id = ca.id
    left join period_length pl on s.id = pl.sentence_id
    where s.sentence_uuid in :sentenceUuids
    and st.sentence_type_uuid = :sentenceTypeUuid
    """,
    nativeQuery = true,
  )
  fun findBySentenceUuidInAndSentenceTypeUuid(sentenceUuids: List<UUID>, sentenceTypeUuid: UUID): List<MissingSentenceInformationDetails>

  @Query(
    value = """
    select count(*) from sentence s
    join charge c on s.charge_id = c.id
    join appearance_charge ac on ac.charge_id = c.id
    join court_appearance ca on ac.appearance_id = ca.id
    join court_case cc on ca.court_case_id = cc.id
    where s.status_id in :sentenceStatuses
    and cc.prisoner_id = :prisonerId
    and c.status_id = :#{#status}
    and ca.status_id = :#{#status}
    and ca.appearance_date <= :beforeOrOnAppearanceDate
    and cc.status_id = :#{#status}
    and s.legacy_data ->> 'bookingId' = :bookingId
  """,
    nativeQuery = true,
  )
  fun countConsecutiveToSentences(
    @Param("prisonerId") prisonerId: String,
    @Param("beforeOrOnAppearanceDate") beforeOrOnAppearanceDate: LocalDate,
    @Param("bookingId") bookingId: String,
    @Param("status") status: String = SentenceEntityStatus.ACTIVE.toString(),
    @Param("sentenceStatuses") statuses: List<String> = listOf(
      SentenceEntityStatus.ACTIVE.toString(),
      SentenceEntityStatus.MANY_CHARGES_DATA_FIX.toString(),
    ),
  ): Long

  @Query(
    """
      select count(*) from SentenceEntity s
      join s.consecutiveTo sct
      join sct.charge sctc
      join sctc.appearanceCharges sctac
      join sctac.appearance sctca
      join sctca.courtCase sctcc
      join s.charge c
      join c.appearanceCharges ac
      join ac.appearance ca on ca != sctca
      join ca.courtCase cc on (cc != sctcc or ca != sctca)
      where sct.sentenceUuid IN :sentenceUuids and
      cc.statusId = :#{#courtCaseStatus} and 
      ca.statusId = :#{#courtAppearanceStatus} and 
      c.statusId = :#{#chargeStatus} and 
      sctc.statusId = :#{#chargeStatus} and 
      sctca.statusId = :#{#courtAppearanceStatus} and
      sctcc.statusId = :#{#courtCaseStatus} and 
      s.statusId in :sentenceStatuses
    """,
  )
  fun countSentencesAfterOnOtherCourtAppearance(
    @Param("sentenceUuids") sentenceUuids: List<UUID>,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<SentenceEntityStatus> = listOf(
      SentenceEntityStatus.ACTIVE,
      SentenceEntityStatus.MANY_CHARGES_DATA_FIX,
      SentenceEntityStatus.INACTIVE,
    ),
  ): Long

  @Query(
    """
      select NEW uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.SentenceAfterOnAnotherCourtAppearanceRow(ca.appearanceUuid, ca.appearanceDate, ca.courtCaseReference, ca.courtCode)
      from SentenceEntity s
      join s.consecutiveTo sct
      join sct.charge sctc
      join sctc.appearanceCharges sctac
      join sctac.appearance sctca
      join sctca.courtCase sctcc
      join s.charge c
      join c.appearanceCharges ac
      join ac.appearance ca on ca != sctca
      join ca.courtCase cc on (cc != sctcc or ca != sctca)
      where sct.sentenceUuid IN :sentenceUuids and 
      cc.statusId = :#{#courtCaseStatus} and 
      ca.statusId = :#{#courtAppearanceStatus} and 
      c.statusId = :#{#chargeStatus} and 
      sctc.statusId = :#{#chargeStatus} and 
      sctca.statusId = :#{#courtAppearanceStatus} and
      sctcc.statusId = :#{#courtCaseStatus} and 
      s.statusId in :sentenceStatuses
    """,
  )
  fun sentencesAfterOnOtherCourtAppearanceDetails(
    @Param("sentenceUuids") sentenceUuids: List<UUID>,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<SentenceEntityStatus> = listOf(
      SentenceEntityStatus.ACTIVE,
      SentenceEntityStatus.MANY_CHARGES_DATA_FIX,
      SentenceEntityStatus.INACTIVE,
    ),
  ): List<SentenceAfterOnAnotherCourtAppearanceRow>

  fun findConsecutiveToSentences(
    @Param("prisonerId") prisonerId: String,
    @Param("beforeOrOnAppearanceDate") beforeOrOnAppearanceDate: LocalDate,
    @Param("bookingId") bookingId: String,
    @Param("status") status: String = SentenceEntityStatus.ACTIVE.toString(),
    @Param("sentenceStatuses") statuses: List<String> = listOf(
      SentenceEntityStatus.ACTIVE.toString(),
      SentenceEntityStatus.MANY_CHARGES_DATA_FIX.toString(),
    ),
  ): List<ConsecutiveToSentenceRow>

  @Query(
    """
    select NEW uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow(cc.prisonerId, cc.caseUniqueIdentifier, ca.appearanceUuid, ca.courtCode, ca.courtCaseReference, ca.appearanceDate, c.chargeUuid, c.offenceCode, c.offenceStartDate, c.offenceEndDate, s.sentenceUuid, s.countNumber, c.legacyData) from SentenceEntity s
    join s.charge c
    join c.appearanceCharges ac
    join ac.appearance ca
    join ca.courtCase cc
    where s.sentenceUuid in :sentenceUuids and
    cc.statusId != :courtCaseStatus and 
    ca.statusId != :courtAppearanceStatus and 
    c.statusId != :chargeStatus and 
    s.statusId != :sentenceStatus
  """,
  )
  fun findConsecutiveToSentenceDetails(
    @Param("sentenceUuids") sentenceUuids: List<UUID>,
    @Param("courtCaseStatus") courtCaseStatus: CourtCaseEntityStatus = CourtCaseEntityStatus.DELETED,
    @Param("courtAppearanceStatus") courtAppearanceStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.DELETED,
    @Param("chargeStatus") chargeStatus: ChargeEntityStatus = ChargeEntityStatus.DELETED,
    @Param("sentenceStatus") sentenceStatus: SentenceEntityStatus = SentenceEntityStatus.DELETED,
  ): List<ConsecutiveToSentenceRow>

  @Query(
    value = """
    WITH RECURSIVE chain(id) AS (
      SELECT s.id
      FROM   sentence s
      WHERE  s.sentence_uuid = :sourceUuid
      UNION ALL
      SELECT child.id
      FROM   sentence child
      JOIN   chain            ON child.consecutive_to_id = chain.id
      JOIN   charge            c  ON c.id = child.charge_id
      JOIN   appearance_charge ac ON ac.charge_id = c.id
      JOIN   court_appearance  ca ON ca.id = ac.appearance_id
      JOIN   court_case        cc ON cc.id = ca.court_case_id
      WHERE  cc.prisoner_id = :prisonerId
        AND  child.consecutive_to_id IS NOT NULL
        AND  child.status_id    = :statusId
        AND  c.status_id     = :statusId
        AND  ca.status_id    = :statusId
        AND  cc.status_id    = :statusId
    )
    SELECT EXISTS (
      SELECT 1
      FROM   chain
      JOIN   sentence s           ON s.id = chain.id
      JOIN   charge c             ON c.id = s.charge_id
      JOIN   appearance_charge ac ON ac.charge_id = c.id
      JOIN   court_appearance ca  ON ca.id = ac.appearance_id
      WHERE  s.sentence_uuid = :targetSentenceId
        AND  ca.appearance_uuid <> :currentAppearanceId   
    ) AS target_in_descendants
  """,
    nativeQuery = true,
  )
  fun isTargetDescendantFromSource(
    @Param("sourceUuid") sourceUuid: UUID,
    @Param("targetSentenceId") targetSentenceId: UUID,
    @Param("prisonerId") prisonerId: String,
    @Param("currentAppearanceId") currentAppearanceId: UUID,
    @Param("statusId") statusId: String = SentenceEntityStatus.ACTIVE.toString(),
  ): Boolean

  fun findViewSentences(
    @Param("prisonerId") prisonerId: String,
    @Param("status") status: String = SentenceEntityStatus.ACTIVE.toString(),
    @Param("sentenceStatuses") sentenceStatuses: List<String> = listOf(
      SentenceEntityStatus.ACTIVE.toString(),
      SentenceEntityStatus.INACTIVE.toString(),
      SentenceEntityStatus.MANY_CHARGES_DATA_FIX.toString(),
    ),
    @Param("courtCaseStatuses") courtCaseStatuses: List<String> = listOf(
      CourtCaseEntityStatus.ACTIVE.toString(),
      CourtCaseEntityStatus.INACTIVE.toString(),
    ),
    @Param("periodLengthStatuses") periodLengthStatuses: List<String> = listOf(
      PeriodLengthEntityStatus.ACTIVE.toString(),
      PeriodLengthEntityStatus.INACTIVE.toString(),
      PeriodLengthEntityStatus.MANY_CHARGES_DATA_FIX.toString(),
    ),
  ): List<ViewSentenceRow>

  @Modifying
  @Query(
    """
    DELETE FROM sentence s
    USING (
      SELECT DISTINCT c.id
        FROM charge c
          JOIN appearance_charge ac ON c.id = ac.charge_id
          JOIN court_appearance a ON ac.appearance_id = a.id
          JOIN court_case cc ON a.court_case_id = cc.id
        WHERE cc.prisoner_id = :prisonerId
    ) del
    WHERE s.charge_id = del.id
  """,
    nativeQuery = true,
  )
  fun deleteByChargeCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    update sentence set consecutive_to_id = null where consecutive_to_id in (
      SELECT s.id
      FROM sentence s 
      JOIN charge c on s.charge_id = c.id
      JOIN appearance_charge ac ON ac.charge_id = c.id
      JOIN court_appearance a ON ac.appearance_id = a.id
      JOIN court_case cc ON a.court_case_id = cc.id
      WHERE cc.prisoner_id = :prisonerId)
  """,
    nativeQuery = true,
  )
  fun updateConsecutiveToIdNullByCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)
}
