package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import java.util.*

interface PeriodLengthRepository : CrudRepository<PeriodLengthEntity, Int> {
  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findFirstByPeriodLengthUuidOrderByUpdatedAtDesc(periodLengthUuid: UUID): PeriodLengthEntity?

  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findByPeriodLengthUuidAndStatusIdNot(periodLengthUuid: UUID, statusId: PeriodLengthEntityStatus = PeriodLengthEntityStatus.DELETED): List<PeriodLengthEntity>

  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid: UUID, statusId: PeriodLengthEntityStatus = PeriodLengthEntityStatus.DELETED): List<PeriodLengthEntity>

  @Modifying
  @Query(
    """
    DELETE FROM period_length
    WHERE sentence_id IN (
      SELECT s.id
      FROM sentence s
       JOIN charge c ON s.charge_id = c.id
       JOIN appearance_charge ac ON c.id = ac.charge_id
       JOIN court_appearance a ON ac.appearance_id = a.id
       join court_case cc on a.court_case_id = cc.id
       where cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteBySentenceCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    DELETE FROM period_length
    WHERE appearance_id IN (
      SELECT a.id
      FROM court_appearance a
      join court_case cc on a.court_case_id = cc.id
      where cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteByAppearanceCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)
}
