package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Used for creating or updating period length records (aka sentence-terms in NOMIS).")
data class LegacyCreatePeriodLength(
  val sentenceUuid: UUID,
  val periodYears: Int?,
  val periodMonths: Int?,
  val periodWeeks: Int?,
  val periodDays: Int?,
  val legacyData: PeriodLengthLegacyData,
  @Schema(description = "Prison identifier where create/update originated from.", required = true, example = "MDI")
  val prisonId: String,
)
