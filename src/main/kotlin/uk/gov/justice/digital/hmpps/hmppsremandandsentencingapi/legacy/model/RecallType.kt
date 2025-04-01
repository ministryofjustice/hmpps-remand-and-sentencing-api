package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

data class RecallType(
  val isRecall: Boolean,
  val type: String,
  val isFixedTermRecall: Boolean,
  val lengthInDays: Int,
)
