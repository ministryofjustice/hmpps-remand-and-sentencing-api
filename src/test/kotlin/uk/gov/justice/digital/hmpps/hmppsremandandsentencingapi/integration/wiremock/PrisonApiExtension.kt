package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.responses.prisonapi.prisonerDetailsResponse

class PrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext?) {
    prisonApi.start()
    prisonApi.stubGetPrisonerDetails("A1234AB")
  }

  override fun beforeEach(context: ExtensionContext?) {
    prisonApi.resetRequests()
  }
  override fun afterAll(context: ExtensionContext?) {
    prisonApi.stop()
  }
}

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8332
  }

  fun stubGetPrisonerDetails(prisonerId: String): StubMapping = stubFor(
    get("/api/offenders/$prisonerId")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(prisonerDetailsResponse())
          .withStatus(200),
      ),
  )
}
