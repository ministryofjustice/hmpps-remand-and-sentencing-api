package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity

data class UpdatedChargeOutcome(
  val entity: ChargeOutcomeEntity,
  val migrateNomisCodeData: Boolean,
)
