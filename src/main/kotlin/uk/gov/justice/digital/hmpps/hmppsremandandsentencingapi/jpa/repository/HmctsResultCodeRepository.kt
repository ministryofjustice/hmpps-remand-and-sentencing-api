package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.HmctsResultCodeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

interface HmctsResultCodeRepository : CrudRepository<HmctsResultCodeEntity, Int> {
  fun findByResultUuid(resultUuid: UUID): HmctsResultCodeEntity?
  fun findByResultCode(nomisCode: String): HmctsResultCodeEntity?
  fun findByResultUuidIn(resultUuids: List<UUID>): List<HmctsResultCodeEntity>
  fun findByStatusIn(statuses: List<ReferenceEntityStatus>): List<HmctsResultCodeEntity>
}
