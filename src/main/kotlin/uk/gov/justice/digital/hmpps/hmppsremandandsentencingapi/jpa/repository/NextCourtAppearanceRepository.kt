package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity

interface NextCourtAppearanceRepository : CrudRepository<NextCourtAppearanceEntity, Int> {
  fun findFirstByFutureSkeletonAppearance(futureSkeletonAppearance: CourtAppearanceEntity): NextCourtAppearanceEntity?

  @Modifying
  @Query(
    """
    delete from NextCourtAppearanceEntity nca
    where nca.futureSkeletonAppearance = :futureSkeletonAppearance
  """,
  )
  fun deleteByFutureSkeletonAppearance(@Param("futureSkeletonAppearance")futureSkeletonAppearance: CourtAppearanceEntity)
}
