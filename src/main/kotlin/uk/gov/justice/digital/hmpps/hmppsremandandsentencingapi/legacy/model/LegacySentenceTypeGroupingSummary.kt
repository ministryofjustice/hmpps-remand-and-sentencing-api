package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

data class LegacySentenceTypeGroupingSummary(
  val nomisSentenceTypeReference: String,
  val nomisDescription: String,
  val isIndeterminate: Boolean,
  val recall: RecallType? = null,
)
