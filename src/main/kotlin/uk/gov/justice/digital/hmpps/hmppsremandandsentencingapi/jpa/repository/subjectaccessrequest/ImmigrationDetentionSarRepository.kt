package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionSarEntity

interface ImmigrationDetentionSarRepository : CrudRepository<ImmigrationDetentionSarEntity, Integer> {

  fun findByPrisonerId(prisonerId: String): List<ImmigrationDetentionSarEntity>
}
