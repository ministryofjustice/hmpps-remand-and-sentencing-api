package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class CourtCaseLegacyData(
  var caseReferences: MutableList<CaseReferenceLegacyData>,
  var bookingId: Long?,
) {
  fun copyFrom(other: CourtCaseLegacyData): CourtCaseLegacyData = CourtCaseLegacyData(caseReferences, other.bookingId)

  companion object {
    fun from(bookingId: Long?) = CourtCaseLegacyData(mutableListOf(), bookingId)
  }
}
