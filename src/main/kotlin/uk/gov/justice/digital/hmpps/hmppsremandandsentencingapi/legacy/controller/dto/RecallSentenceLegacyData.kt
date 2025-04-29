package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class RecallSentenceLegacyData(
  val sentenceCalcType: String?,
  val sentenceCategory: String?,
  val sentenceTypeDesc: String?,
  val postedDate: String,
  var active: Boolean?,
) {
  companion object {
    fun from(legacyData: SentenceLegacyData): RecallSentenceLegacyData = RecallSentenceLegacyData(
      legacyData.sentenceCalcType,
      legacyData.sentenceCategory,
      legacyData.sentenceTypeDesc,
      legacyData.postedDate,
      legacyData.active,
    )
  }
}
