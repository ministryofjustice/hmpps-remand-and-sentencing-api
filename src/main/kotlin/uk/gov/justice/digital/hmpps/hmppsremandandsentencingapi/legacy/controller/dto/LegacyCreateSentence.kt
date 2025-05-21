package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.util.UUID

data class LegacyCreateSentence(
  val chargeUuids: List<UUID>,
  val chargeNumber: String?,
  val fine: LegacyCreateFine? = null,
  val consecutiveToLifetimeUuid: UUID? = null,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val returnToCustodyDate: LocalDate? = null,
)
