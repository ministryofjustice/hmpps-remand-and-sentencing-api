package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class HmctsCourtHearing(
  val hearingId: UUID,
  val courtName: String,
  val courtId: UUID,
  val courtCode: String? = null,
  val hearingDate: LocalDate,
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
  val chargeId: UUID,
  val listingNumber: Int?,
  val offenceLegislation: String?,
  val code: String,
  val pleaDate: LocalDate?,
  val pleaValue: String?,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  val title: String,
  val wording: String,
  val convictionDate: LocalDate?,
  val results: List<HmctsCourtResult>,
)

data class HmctsCourtResult(
  val code: String,
  val description: String,
  val keyValuePairs: List<ResultKeyValue>,
) {
  fun findValue(key: HmctsResultKeys): String? = keyValuePairs.firstOrNull { it.key == key.key }?.value
}

data class ResultKeyValue(
  val key: String,
  val value: String?,
)

data class HmctsNextCourtHearing(
  val courtName: String,
  val hmctsCourtId: UUID,
  val hmppsCourtId: String? = null,
  val hearingDate: LocalDateTime?,
  val hearingId: UUID?,
)

enum class HmctsResultKeys(val key: String) {
  APPROPRIATE_CUSTODIAL_TERM("Appropriate custodial term"),
  TAGGED_BAIL("Bail remand days to count (tagged days)"),
  CONCURRENT("Concurrent"),
  CONSECUTIVE_TO_OFFENCE("Consecutive to offence"),
  CUSTODIAL_PERIOD("Custodial period"),
  EXTENSION_PERIOD("Extension period"),
  EXTENSION_PERIOD_SECTION_35A("Extension period section 35A (immediate custodial sentence)"),
  IMPRISONMENT_PERIOD("Imprisonment period"),
  FOREIGN_JURISDICTION("Number of days in custody in foreign jurisdiction to count"),
  FOREIGN_POWER_SECTION_31("This offence is aggravated by the foreign power condition being met in relation to it as defined by section 31 of the National Security Act 2023"),
  TOTAL_CUSTODIAL_PERIOD("Total custodial period"),
  WHICH_IS_ON_CASE_NUMBER("Which is on case number"),
}
