package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
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

  fun updateDocumentMetadata(
    prisonerId: String,
    documentId: String,
    uploadStatus: DocumentMetadataStatus,
  ) {
    webClient
      .put()
      .uri("/documents/{documentId}/metadata", documentId)
      .header("Service-Name", "Remand and Sentencing")
      .bodyValue(
        mapOf(
          "prisonerId" to prisonerId,
          "source" to "RemandSentencingUser",
          "status" to uploadStatus,
        ),
      )
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
