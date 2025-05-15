package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class LegacySearchSentence(
  val lifetimeUuids: List<UUID>,
)
