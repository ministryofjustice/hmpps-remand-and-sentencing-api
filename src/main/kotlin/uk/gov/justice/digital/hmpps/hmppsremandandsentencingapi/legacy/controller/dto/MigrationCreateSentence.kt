package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  @Schema(deprecated = true, description = "Deprecated. Will be removed soon.")
  val chargeNumber: String?,
  val fine: MigrationCreateFine?,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val consecutiveToSentenceId: MigrationSentenceId?,
  val periodLengths: List<MigrationCreatePeriodLength>,
  val returnToCustodyDate: LocalDate? = null,
)
