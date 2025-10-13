package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import com.fasterxml.jackson.annotation.JsonIgnore

data class SearchDocuments(
  val warrantTypeDocumentTypes: List<String> = listOf(),
  val keyword: String?,
  val courtCode: String?,
) {
  @JsonIgnore
  fun isEmpty(): Boolean = warrantTypeDocumentTypes.isEmpty() && keyword?.takeUnless { it.isEmpty() } == null
}
