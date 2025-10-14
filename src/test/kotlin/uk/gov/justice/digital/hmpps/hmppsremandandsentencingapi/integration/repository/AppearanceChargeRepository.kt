package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable.AppearanceChargeId
import java.util.UUID

interface AppearanceChargeRepository : JpaRepository<AppearanceChargeEntity, AppearanceChargeId> {

  fun countByAppearanceAppearanceUuid(appearanceUuid: UUID): Int
}
