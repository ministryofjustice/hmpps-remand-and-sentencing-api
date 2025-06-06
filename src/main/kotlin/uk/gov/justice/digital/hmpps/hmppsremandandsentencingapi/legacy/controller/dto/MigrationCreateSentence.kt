package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  val fine: MigrationCreateFine?,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val consecutiveToSentenceId: MigrationSentenceId?,
  val periodLengths: List<MigrationCreatePeriodLength>,
  val returnToCustodyDate: LocalDate? = null,
)
