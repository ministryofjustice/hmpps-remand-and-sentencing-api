package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

import java.time.LocalDate

data class LegacySentenceTypeGroupingSummary(
  val nomisSentenceTypeReference: String,
  val nomisDescription: String,
  val isIndeterminate: Boolean,
  val recall: RecallType?,
  val nomisActive: Boolean,
  val nomisExpiryDate: LocalDate?,
)
