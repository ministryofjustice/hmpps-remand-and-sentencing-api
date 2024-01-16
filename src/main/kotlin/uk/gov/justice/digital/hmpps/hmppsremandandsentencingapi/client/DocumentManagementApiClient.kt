package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentMetadata

@Component
class DocumentManagementApiClient(@Qualifier("documentManagementApiWebClient") private val webClient: WebClient) {

  fun putDocumentMetadata(documentId: String, metadata: DocumentMetadata) {
    webClient
      .put()
      .uri("/documents/{documentId}/metadata", documentId)
      .body(Mono.just(metadata), DocumentMetadata::class.java)
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
