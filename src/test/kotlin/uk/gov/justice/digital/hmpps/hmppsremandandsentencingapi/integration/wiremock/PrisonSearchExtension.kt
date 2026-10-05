package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.TestUtil
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails

class PrisonSearchExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val prisonSearch = PrisonSearchMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    prisonSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonSearch.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    prisonSearch.stop()
  }
}

class PrisonSearchMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8557
  }

  fun stubPrisonSearchDetails(prisonerId: String, prisonDetails: PrisonSearchDetails): StubMapping = stubFor(
    get("/prisoner/$prisonerId")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(TestUtil.objectMapper().writeValueAsString(prisonDetails))
          .withStatus(200),
      ),
  )
}
