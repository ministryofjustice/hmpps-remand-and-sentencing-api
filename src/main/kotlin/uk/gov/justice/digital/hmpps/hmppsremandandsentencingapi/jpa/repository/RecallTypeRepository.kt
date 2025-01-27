package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType

interface RecallTypeRepository : CrudRepository<RecallTypeEntity, Int> {
  fun findOneByCode(code: RecallType): RecallTypeEntity?
}
