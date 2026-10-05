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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails

class ManageOffencesApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val manageOffencesApi = ManageOffencesMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    manageOffencesApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageOffencesApi.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    manageOffencesApi.stop()
  }
}

class ManageOffencesMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8556
  }

  fun stubManageOffences(offenceCode: String, offenceDetails: OffenceDetails): StubMapping = stubFor(
    get("/offences/code/unique/$offenceCode")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(TestUtil.objectMapper().writeValueAsString(offenceDetails))
          .withStatus(200),
      ),
  )
}
