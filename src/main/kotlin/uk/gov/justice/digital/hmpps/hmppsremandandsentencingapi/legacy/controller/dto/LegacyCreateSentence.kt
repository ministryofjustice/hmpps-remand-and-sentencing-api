package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.util.*

data class LegacyCreateSentence(
  val chargeUuids: List<UUID>,
  val appearanceUuid: UUID,
  val fine: LegacyCreateFine? = null,
  val consecutiveToLifetimeUuid: UUID? = null,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val returnToCustodyDate: LocalDate? = null,
)
