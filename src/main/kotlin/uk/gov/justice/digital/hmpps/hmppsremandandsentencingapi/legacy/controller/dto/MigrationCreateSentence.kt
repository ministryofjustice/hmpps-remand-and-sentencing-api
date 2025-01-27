package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class MigrationCreateSentence(
  val sentenceId: MigrationSentenceId,
  val chargeNumber: String?,
  val fine: MigrationCreateFine?,
  val active: Boolean,
  val legacyData: SentenceLegacyData,
  @Schema(description = "The consecutive to sentence Id if the sentence is in the same court case")
  val consecutiveToSentenceId: MigrationSentenceId?,
  @Schema(description = "The consecutive to lifetime uuid if the sentence is not in the same court case")
  val consecutiveToSentenceLifetimeUuid: UUID?,
  val periodLengths: List<MigrationCreatePeriodLength>,
)
