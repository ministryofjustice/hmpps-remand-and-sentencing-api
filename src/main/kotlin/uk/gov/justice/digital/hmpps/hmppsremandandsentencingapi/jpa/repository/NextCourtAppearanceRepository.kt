package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity

interface NextCourtAppearanceRepository : CrudRepository<NextCourtAppearanceEntity, Int> {
  @Modifying
  @Query("DELETE FROM NextCourtAppearanceEntity n WHERE n.id IN :ids")
  fun deleteAllByIdIn(@Param("ids") ids: List<Int>)
}
