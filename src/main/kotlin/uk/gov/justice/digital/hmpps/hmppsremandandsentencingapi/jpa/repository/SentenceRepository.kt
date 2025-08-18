package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.SentenceAfterOnAnotherCourtAppearanceRow
import java.time.LocalDate
import java.util.*

interface SentenceRepository : CrudRepository<SentenceEntity, Int> {
  fun findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid: UUID): SentenceEntity?

  fun findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(sentenceUuid: UUID, chargeUUID: UUID): SentenceEntity?

  fun findBySentenceUuidAndChargeChargeUuidNotInAndStatusIdNot(sentenceUuid: UUID, chargeUuids: List<UUID>, statusId: EntityStatus = EntityStatus.DELETED): List<SentenceEntity>

  fun findBySentenceUuidIn(sentenceUuids: List<UUID>): List<SentenceEntity>

  fun findBySentenceUuid(sentenceUuid: UUID): List<SentenceEntity>

  fun findBySentenceUuidAndStatusId(sentenceUuid: UUID, status: EntityStatus): List<SentenceEntity>

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
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<EntityStatus> = listOf(
      EntityStatus.ACTIVE,
      EntityStatus.MANY_CHARGES_DATA_FIX,
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
      
      where sct.sentenceUuid = :sentenceUuid
      and c.statusId = :#{#status}
      and ca.statusId = :#{#status}
      and cc.statusId = :#{#status}
      and sctc.statusId = :#{#status}
      and sctca.statusId = :#{#status}
      and sctcc.statusId = :#{#status}
      and s.statusId in :sentenceStatuses
    """,
  )
  fun countSentencesAfterOnOtherCourtAppearance(
    @Param("sentenceUuid") sentenceUuid: UUID,
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<EntityStatus> = listOf(
      EntityStatus.ACTIVE,
      EntityStatus.MANY_CHARGES_DATA_FIX,
      EntityStatus.INACTIVE,
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
      
      where sct.sentenceUuid = :sentenceUuid
      and c.statusId = :#{#status}
      and ca.statusId = :#{#status}
      and cc.statusId = :#{#status}
      and sctc.statusId = :#{#status}
      and sctca.statusId = :#{#status}
      and sctcc.statusId = :#{#status}
      and s.statusId in :sentenceStatuses
    """,
  )
  fun sentencesAfterOnOtherCourtAppearanceDetails(
    @Param("sentenceUuid") sentenceUuid: UUID,
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<EntityStatus> = listOf(
      EntityStatus.ACTIVE,
      EntityStatus.MANY_CHARGES_DATA_FIX,
      EntityStatus.INACTIVE,
    ),
  ): List<SentenceAfterOnAnotherCourtAppearanceRow>

  fun findConsecutiveToSentences(
    @Param("prisonerId") prisonerId: String,
    @Param("beforeOrOnAppearanceDate") beforeOrOnAppearanceDate: LocalDate,
    @Param("bookingId") bookingId: String,
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<EntityStatus> = listOf(
      EntityStatus.ACTIVE,
      EntityStatus.MANY_CHARGES_DATA_FIX,
    ),
  ): List<ConsecutiveToSentenceRow>

  @Query(
    """
    select NEW uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow(cc.prisonerId, cc.caseUniqueIdentifier, ca.appearanceUuid, ca.courtCode, ca.courtCaseReference, ca.appearanceDate, c.chargeUuid, c.offenceCode, c.offenceStartDate, c.offenceEndDate, s.sentenceUuid, s.countNumber, c.legacyData) from SentenceEntity s
    join s.charge c
    join c.appearanceCharges ac
    join ac.appearance ca
    join ca.courtCase cc
    where s.statusId != :#{#status}
    and s.sentenceUuid in :sentenceUuids
    and c.statusId != :#{#status}
    and ca.statusId != :#{#status}
    and cc.statusId != :#{#status}
  """,
  )
  fun findConsecutiveToSentenceDetails(
    @Param("sentenceUuids") sentenceUuids: List<UUID>,
    @Param("status") status: EntityStatus = EntityStatus.DELETED,
  ): List<ConsecutiveToSentenceRow>
}
