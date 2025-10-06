package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import com.fasterxml.jackson.annotation.JsonIgnore

data class SearchDocuments(
  val warrantTypeDocumentTypes: List<String> = listOf(),
  val caseReference: String?,
) {
  @JsonIgnore
  fun isEmpty(): Boolean = warrantTypeDocumentTypes.isEmpty() && caseReference?.takeUnless { it.isEmpty() } == null
}
