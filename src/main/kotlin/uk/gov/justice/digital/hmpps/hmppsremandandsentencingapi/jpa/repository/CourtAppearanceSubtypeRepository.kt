package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceSubtypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.*

interface CourtAppearanceSubtypeRepository : JpaRepository<CourtAppearanceSubtypeEntity, Int> {

  fun findByAppearanceSubtypeUuid(appearanceTypeUuid: UUID): CourtAppearanceSubtypeEntity?

  fun findByStatusIn(statuses: List<ReferenceEntityStatus>): List<CourtAppearanceSubtypeEntity>
}
