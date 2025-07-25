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
    """
    select count(*) from SentenceEntity s
    join s.charge c
    join c.appearanceCharges ac
    join ac.appearance ca
    join ca.courtCase cc
    where s.statusId in :sentenceStatuses
    and cc.prisonerId = :prisonerId
    and c.statusId = :#{#status}
    and ca.statusId = :#{#status}
    and ca.appearanceDate <= :beforeOrOnAppearanceDate
    and cc.statusId = :#{#status}
  """,
  )
  fun countConsecutiveToSentences(
    @Param("prisonerId") prisonerId: String,
    @Param("beforeOrOnAppearanceDate") beforeOrOnAppearanceDate: LocalDate,
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

  @Query(
    """
    select NEW uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow(cc.prisonerId, cc.caseUniqueIdentifier, ca.appearanceUuid, ca.courtCode, ca.courtCaseReference, ca.appearanceDate, c, s) from SentenceEntity s
    left join fetch s.sentenceType st
    join s.charge c
    left join fetch c.chargeOutcome co
    join c.appearanceCharges ac
    join ac.appearance ca
    join ca.courtCase cc
    where s.statusId in :sentenceStatuses
    and cc.prisonerId = :prisonerId
    and c.statusId = :#{#status}
    and ca.statusId = :#{#status}
    and ca.appearanceDate <= :beforeOrOnAppearanceDate
    and cc.statusId = :#{#status}
  """,
  )
  fun findConsecutiveToSentences(
    @Param("prisonerId") prisonerId: String,
    @Param("beforeOrOnAppearanceDate") beforeOrOnAppearanceDate: LocalDate,
    @Param("status") status: EntityStatus = EntityStatus.ACTIVE,
    @Param("sentenceStatuses") statuses: List<EntityStatus> = listOf(
      EntityStatus.ACTIVE,
      EntityStatus.MANY_CHARGES_DATA_FIX,
    ),
  ): List<ConsecutiveToSentenceRow>

  @Query(
    """
    select NEW uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow(cc.prisonerId, cc.caseUniqueIdentifier, ca.appearanceUuid, ca.courtCode, ca.courtCaseReference, ca.appearanceDate, c, s) from SentenceEntity s
    left join fetch s.sentenceType st
    join s.charge c
    left join fetch c.chargeOutcome co
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
