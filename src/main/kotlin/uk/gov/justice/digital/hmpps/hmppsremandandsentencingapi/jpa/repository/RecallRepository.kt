package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.*

interface RecallRepository : CrudRepository<RecallEntity, Int> {
  fun findOneByRecallUuid(recallUuid: UUID): RecallEntity?
  fun findByPrisonerIdAndStatusId(prisonerId: String, statusId: EntityStatus = EntityStatus.ACTIVE): List<RecallEntity>
  fun findByPrisonerId(prisonerId: String): List<RecallEntity>

  @Query(
    value = """
    select distinct r.*
    from recall r
      join recall_sentence rs on rs.recall_id = r.id
      join sentence s on s.id = rs.sentence_id
    where r.prisoner_id = :fromNoms
      and s.legacy_data ->> 'bookingId' = :bookingId
  """,
    nativeQuery = true,
  )
  fun findByPrisonerIdAndBookingId(
    @Param("fromNoms") fromNoms: String,
    @Param("bookingId") bookingId: String,
  ): List<RecallEntity>
}
