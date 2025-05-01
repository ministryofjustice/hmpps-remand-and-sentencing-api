package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import java.util.UUID

interface PeriodLengthRepository : CrudRepository<PeriodLengthEntity, Int> {
  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findFirstByPeriodLengthUuidOrderByUpdatedAtDesc(periodLengthUuid: UUID): PeriodLengthEntity?
  @EntityGraph(attributePaths = ["sentenceEntity"])
  fun findByPeriodLengthUuid(periodLengthUuid: UUID): List<PeriodLengthEntity>
}
