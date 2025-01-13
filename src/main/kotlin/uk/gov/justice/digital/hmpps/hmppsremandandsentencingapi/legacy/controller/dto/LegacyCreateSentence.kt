package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class LegacyCreateSentence(
  val chargeLifetimeUuid: UUID,
  val chargeNumber: String?,
  val fine: LegacyCreateFine?,
  val active: Boolean,
  val prisonId: String,
  val legacyData: SentenceLegacyData,
)
