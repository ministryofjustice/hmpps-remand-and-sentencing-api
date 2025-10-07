package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity

interface NextCourtAppearanceRepository : CrudRepository<NextCourtAppearanceEntity, Int> {
  fun findFirstByFutureSkeletonAppearance(futureSkeletonAppearance: CourtAppearanceEntity): NextCourtAppearanceEntity?
}
