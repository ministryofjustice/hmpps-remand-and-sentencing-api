package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.DocumentMetadataStatus

@Component
class DocumentManagementApiClient(@Qualifier("documentManagementApiWebClient") private val webClient: WebClient) {

  fun deleteDocument(documentId: String) {
    webClient
      .delete()
      .uri("/documents/{documentId}", documentId)
      .header("Service-Name", "Remand and Sentencing")
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  @Deprecated("Use updateDocumentMetadata instead")
  fun setDocumentStatus(
    documentId: String,
    status: DocumentMetadataStatus,
  ) {
    webClient
      .patch()
      .uri("/documents/{documentId}/metadata", documentId)
      .header("Service-Name", "Remand and Sentencing")
      .bodyValue(
        mapOf(
          "status" to status,
        ),
      )
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun updateDocumentMetadata(
    documentId: String,
    status: DocumentMetadataStatus,
    caseReferences: Set<String>,
  ) {
    webClient
      .patch()
      .uri("/documents/{documentId}/metadata", documentId)
      .header("Service-Name", "Remand and Sentencing")
      .bodyValue(buildDocumentMetadataPath(status, caseReferences))
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  private fun buildDocumentMetadataPath(status: DocumentMetadataStatus, caseReferences: Set<String>): Map<String, Any> {
    val baseMetadata = mutableMapOf<String, Any>(
      "status" to status,
      "isUnread" to false,
    )
    caseReferences.takeIf { it.isNotEmpty() }?.let { baseMetadata["caseReferences"] = it.toList() }
    return baseMetadata
  }

  fun getDocumentsByIds(documentIds: List<String>): List<DocumentManagementApiDocument> = webClient
    .post()
    .uri("/documents")
    .header("Service-Name", "Remand and Sentencing")
    .bodyValue(documentIds)
    .retrieve()
    .bodyToMono(typeReference<List<DocumentManagementApiDocument>>())
    .block()!!
}
