package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class CourtCaseLegacyData(
  val caseReferences: MutableList<CaseReferenceLegacyData>,
  val bookingId: Long?,
) {
  fun copyFrom(other: CourtCaseLegacyData): CourtCaseLegacyData = CourtCaseLegacyData(caseReferences, other.bookingId)

  companion object {
    fun from(bookingId: Long?) = CourtCaseLegacyData(mutableListOf(), bookingId)
  }
}
