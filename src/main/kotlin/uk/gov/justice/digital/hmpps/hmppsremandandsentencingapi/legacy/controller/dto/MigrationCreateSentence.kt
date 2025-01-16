package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  val chargeNumber: String?,
  val fine: MigrationCreateFine?,
  val active: Boolean,
  val legacyData: SentenceLegacyData,
)
