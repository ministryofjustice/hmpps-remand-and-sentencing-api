package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class RefreshCaseReferences(
  val caseReferences: MutableList<CaseReferenceLegacyData>,
  val performedByUser: String?,
)
