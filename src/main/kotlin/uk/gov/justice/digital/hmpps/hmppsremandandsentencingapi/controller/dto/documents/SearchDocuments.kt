package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

data class SearchDocuments(
  val warrantTypeDocumentTypes: List<String> = listOf(),
  val caseReference: String?,
)
