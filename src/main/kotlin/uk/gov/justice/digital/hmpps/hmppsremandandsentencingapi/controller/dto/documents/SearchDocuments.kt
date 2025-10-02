package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

data class SearchDocuments(
  val warrantTypeDocumentTypes: List<String> = listOf(),
  val caseReference: String?,
) {
  fun isEmpty(): Boolean = warrantTypeDocumentTypes.isEmpty() && caseReference?.takeUnless { it.isEmpty() } == null
}
