package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionSarEntity

@ConditionalOnProperty(
  prefix = "hmpps.sar",
  name = ["enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
interface ImmigrationDetentionSarRepository : CrudRepository<ImmigrationDetentionSarEntity, String> {

  fun findImmigrationDetentionSarEntitiesByPrisonerId(prisonerId: String): List<ImmigrationDetentionSarEntity>
}
