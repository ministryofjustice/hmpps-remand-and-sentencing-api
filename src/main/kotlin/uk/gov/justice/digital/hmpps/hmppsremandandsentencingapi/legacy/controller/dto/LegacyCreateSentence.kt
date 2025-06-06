package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

data class LegacyCreateSentence(
  val chargeUuids: List<UUID>,
  @Schema(deprecated = true, description = "Deprecated. Will be removed soon.")
  val chargeNumber: String? = null,
  val fine: LegacyCreateFine? = null,
  val consecutiveToLifetimeUuid: UUID? = null,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val returnToCustodyDate: LocalDate? = null,
)
