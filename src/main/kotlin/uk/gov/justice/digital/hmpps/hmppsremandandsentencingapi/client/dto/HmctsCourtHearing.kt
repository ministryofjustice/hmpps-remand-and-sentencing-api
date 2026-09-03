package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class HmctsCourtHearing(
  val hearingId: UUID,
  val courtName: String,
  val courtId: UUID,
  val hearingDate: LocalDateTime,
  val caseReferences: List<String>,
  val hearingType: String,
  val documents: List<HmctsCourHearingDocument>,
  val charges: List<HmctsCourtCharge> = emptyList(),
  val nextHearing: HmctsNextCourtHearing? = null,
) {
  fun isRemandHearing() = documents.any { it.isRemandWarrant() }
  fun isSentenceHearing() = documents.any { it.isSentenceWarrant() }
}

data class HmctsCourHearingDocument(
  val documentType: String,
  val documentId: UUID,
) {
  fun isWarrant() = isRemandWarrant() || isSentenceWarrant()
  fun isRemandWarrant() = documentType == "REMAND_WARRANT"
  fun isSentenceWarrant() = documentType == "SENTENCING_WARRANT"
}

data class HmctsCourtCharge(
  val listingNumber: Int,
  val offenceLegislation: String,
  val code: String,
  val pleaDate: LocalDate,
  val pleaValue: String,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  val title: String,
  val wording: String,
  val results: List<HmctsCourtResult>,
)

data class HmctsCourtResult(
  val code: String,
  val description: String,

)

data class HmctsNextCourtHearing(
  val courtName: String,
  val hmctsCourtId: UUID,
  val hmppsCourtId: String? = null,
  val hearingDate: LocalDateTime,
)
