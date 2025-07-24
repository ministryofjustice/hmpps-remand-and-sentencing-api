package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class CourtCaseLegacyData(
  val caseReferences: MutableList<CaseReferenceLegacyData>,
  val bookingId: Long?,
)
