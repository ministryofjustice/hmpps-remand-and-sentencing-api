package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class LegacyCreateSentence(
  val chargeLifetimeUuid: UUID,
  val chargeNumber: String?,
  val fine: LegacyCreateFine?,
  val consecutiveToLifetimeUuid: UUID?,
  val active: Boolean,
  val prisonId: String,
  val legacyData: SentenceLegacyData,
)
