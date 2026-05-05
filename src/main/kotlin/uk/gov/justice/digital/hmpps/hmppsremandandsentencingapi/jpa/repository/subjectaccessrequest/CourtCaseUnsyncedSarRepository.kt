package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.CourtCaseUnsyncedSarEntity

@ConditionalOnSarEnabled
interface CourtCaseUnsyncedSarRepository : CrudRepository<CourtCaseUnsyncedSarEntity, String> {

  fun existsByPrisonerId(prisonerId: String): Boolean
}
