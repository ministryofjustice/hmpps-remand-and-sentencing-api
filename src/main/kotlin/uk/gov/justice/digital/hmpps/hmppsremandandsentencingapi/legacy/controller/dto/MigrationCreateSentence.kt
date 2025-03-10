package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  val chargeNumber: String?,
  val fine: MigrationCreateFine?,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val consecutiveToSentenceId: MigrationSentenceId?,
  val periodLengths: List<MigrationCreatePeriodLength>,
)
