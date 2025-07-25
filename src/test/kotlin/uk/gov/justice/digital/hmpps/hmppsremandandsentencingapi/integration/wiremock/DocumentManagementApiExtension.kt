package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi.documentMetadataRequest

class DocumentManagementApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val documentManagementApi = DocumentManagementApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext?) {
    documentManagementApi.start()
    documentManagementApi.stubPutDocumentMetadata("123", "PRI123")
    documentManagementApi.stubPutDocumentMetadata("123", "OTHERPRISONER")
  }

  override fun beforeEach(context: ExtensionContext?) {
    documentManagementApi.resetRequests()
  }
  override fun afterAll(context: ExtensionContext?) {
    documentManagementApi.stop()
  }
}

class DocumentManagementApiMockServer : WireMockServer(WireMockConfiguration.options().port(WIREMOCK_PORT).notifier(ConsoleNotifier(false))) {
  companion object {
    private const val WIREMOCK_PORT = 8442
  }

  fun stubPutDocumentMetadata(documentId: String, prisonerId: String): StubMapping = stubFor(
    put("/documents/$documentId/metadata")
      .withRequestBody(equalToJson(documentMetadataRequest(prisonerId)))
      .willReturn(
        aResponse()
          .withStatus(200),
      ),
  )

  fun stubDeleteDocument(documentId: String): StubMapping = stubFor(
    delete("/documents/$documentId")
      .willReturn(
        aResponse()
          .withStatus(204),
      ),
  )
}
