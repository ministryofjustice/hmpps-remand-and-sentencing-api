package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  val chargeNumber: String?,
  val fine: MigrationCreateFine?,
  val active: Boolean,
  val legacyData: SentenceLegacyData,
  val consecutiveToSentenceId: MigrationSentenceId?,
  val consecutiveToSentenceLifetimeUuid: UUID?,
)
