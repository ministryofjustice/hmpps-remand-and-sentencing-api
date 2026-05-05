package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionUnsyncedSarEntity

@ConditionalOnSarEnabled
interface ImmigrationDetentionUnsyncedSarRepository : CrudRepository<ImmigrationDetentionUnsyncedSarEntity, String> {

  fun findImmigrationDetentionSarEntitiesByPrisonerId(prisonerId: String): List<ImmigrationDetentionUnsyncedSarEntity>
}
