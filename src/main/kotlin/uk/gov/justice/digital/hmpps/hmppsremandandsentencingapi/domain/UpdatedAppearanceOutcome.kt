package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity

data class UpdatedAppearanceOutcome(
  val entity: AppearanceOutcomeEntity,
  val migrateNomisCodeData: Boolean,
)
