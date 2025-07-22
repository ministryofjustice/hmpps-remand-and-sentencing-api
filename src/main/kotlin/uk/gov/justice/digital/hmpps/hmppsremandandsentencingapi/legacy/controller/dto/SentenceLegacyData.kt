package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class SentenceLegacyData(
  val sentenceCalcType: String? = null,
  val sentenceCategory: String? = null,
  val sentenceTypeDesc: String? = null,
  val postedDate: String,
  var active: Boolean? = null,
  var nomisLineReference: String? = null,
  val bookingId: Long?,
) {
  fun isSame(other: SentenceLegacyData?): Boolean = sentenceCalcType == other?.sentenceCalcType &&
    sentenceCategory == other?.sentenceCategory &&
    sentenceTypeDesc == other?.sentenceTypeDesc &&
    active == other?.active &&
    nomisLineReference == other?.nomisLineReference &&
    bookingId == other?.bookingId
}
