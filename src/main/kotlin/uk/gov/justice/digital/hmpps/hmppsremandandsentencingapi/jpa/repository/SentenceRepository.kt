package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.util.UUID

interface SentenceRepository : CrudRepository<SentenceEntity, Int> {
  fun findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid: UUID): SentenceEntity?

  fun findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(sentenceUuid: UUID, chargeUUID: UUID): SentenceEntity?

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
}
