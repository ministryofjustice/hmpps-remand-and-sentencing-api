package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

  companion object {
    fun from(updateSentenceBookingId: LegacyUpdateSentenceBookingId): SentenceLegacyData = SentenceLegacyData(
      postedDate = LocalDateTime.now().format(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
      ),
      bookingId = updateSentenceBookingId.bookingId,
    )
  }
}
