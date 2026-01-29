package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DocumentManagementApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val documentManagementApi = DocumentManagementApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    documentManagementApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    documentManagementApi.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    documentManagementApi.stop()
  }
}

class DocumentManagementApiMockServer : WireMockServer(WireMockConfiguration.options().port(WIREMOCK_PORT).notifier(ConsoleNotifier(false))) {
  companion object {
    private const val WIREMOCK_PORT = 8442
  }

  fun stubDeleteDocument(documentId: String): StubMapping = stubFor(
    delete("/documents/$documentId")
      .willReturn(
        aResponse()
          .withStatus(204),
      ),
  )
}
