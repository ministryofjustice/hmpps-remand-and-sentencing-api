package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

interface ChargeOutcomeRepository : CrudRepository<ChargeOutcomeEntity, Int> {
  fun findByOutcomeUuid(outcomeUuid: UUID): ChargeOutcomeEntity?

  fun findByNomisCode(nomisCode: String): ChargeOutcomeEntity?

  fun findByOutcomeUuidIn(outcomeUuids: List<UUID>): List<ChargeOutcomeEntity>

  fun findByNomisCodeIn(nomisCodes: List<String>): List<ChargeOutcomeEntity>

  fun findByStatusIn(statuses: List<ReferenceEntityStatus>): List<ChargeOutcomeEntity>

  @Query("select distinct outcomeType from ChargeOutcomeEntity")
  fun findDistinctOutcomeTypes(): List<String>

  @Query("select distinct dispositionCode from ChargeOutcomeEntity")
  fun findDistinctDispositionCodes(): List<String>
}
