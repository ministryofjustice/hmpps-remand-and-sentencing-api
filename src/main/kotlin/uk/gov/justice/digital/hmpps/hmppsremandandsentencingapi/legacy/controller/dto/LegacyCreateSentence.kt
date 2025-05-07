package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.util.UUID

data class LegacyCreateSentence(
  val chargeUuids: List<UUID>,
  val chargeNumber: String?,
  val fine: LegacyCreateFine?,
  val consecutiveToLifetimeUuid: UUID?,
  val active: Boolean,
  val prisonId: String,
  var legacyData: SentenceLegacyData,
  val returnToCustodyDate: LocalDate? = null,
)
