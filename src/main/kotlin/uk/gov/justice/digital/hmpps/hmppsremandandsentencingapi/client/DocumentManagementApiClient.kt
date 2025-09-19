package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

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
}
