package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.math.BigDecimal

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  val chargeNumber: String?,
  val fineAmount: BigDecimal?,
  val active: Boolean,
  val legacyData: SentenceLegacyData,
)
