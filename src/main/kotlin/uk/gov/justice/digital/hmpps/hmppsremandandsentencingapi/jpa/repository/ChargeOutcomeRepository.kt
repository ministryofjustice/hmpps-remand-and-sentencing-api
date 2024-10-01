package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import java.util.UUID

interface ChargeOutcomeRepository : CrudRepository<ChargeOutcomeEntity, Int> {
  fun findByOutcomeUuid(outcomeUuid: UUID): ChargeOutcomeEntity?
}
