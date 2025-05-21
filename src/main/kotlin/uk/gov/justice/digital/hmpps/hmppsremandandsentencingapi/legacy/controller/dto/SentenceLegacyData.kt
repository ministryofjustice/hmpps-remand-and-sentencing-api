package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class SentenceLegacyData(
  val sentenceCalcType: String? = null,
  val sentenceCategory: String? = null,
  val sentenceTypeDesc: String? = null,
  val postedDate: String,
  var active: Boolean? = null,
)
