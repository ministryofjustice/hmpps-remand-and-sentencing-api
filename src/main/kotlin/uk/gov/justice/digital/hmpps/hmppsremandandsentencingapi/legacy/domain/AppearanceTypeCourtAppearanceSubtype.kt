package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceSubtypeEntity

data class AppearanceTypeCourtAppearanceSubtype(
  val appearanceType: AppearanceTypeEntity,
  val courtAppearanceSubtype: CourtAppearanceSubtypeEntity? = null,
)
