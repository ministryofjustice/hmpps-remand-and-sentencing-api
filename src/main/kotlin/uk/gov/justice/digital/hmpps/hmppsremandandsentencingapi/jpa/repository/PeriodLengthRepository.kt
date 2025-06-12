package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus.DELETED
import java.util.UUID

interface PeriodLengthRepository : CrudRepository<PeriodLengthEntity, Int> {
  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findFirstByPeriodLengthUuidOrderByUpdatedAtDesc(periodLengthUuid: UUID): PeriodLengthEntity?

  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findByPeriodLengthUuid(periodLengthUuid: UUID): List<PeriodLengthEntity>

  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid: UUID, statusId: EntityStatus = DELETED): List<PeriodLengthEntity>

  @Modifying
  @Query(
    """
        DELETE FROM PeriodLengthEntity pl 
        WHERE pl.sentenceEntity IN (
            SELECT s FROM SentenceEntity s 
            JOIN s.charge c 
            JOIN c.appearanceCharges ac 
            JOIN ac.appearance a 
            WHERE a.courtCase.id = :caseId
        )
    """,
  )
  fun deleteAllBySentenceEntityChargeAppearanceCourtCaseId(@Param("caseId") caseId: Int)
}
