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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails

class PrisonRegisterExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val prisonRegister = PrisonRegisterMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    prisonRegister.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonRegister.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    prisonRegister.stop()
  }
}

class PrisonRegisterMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8555
  }

  fun stubGetAgencyDetails(agencyId: String, agencyDetails: AgencyDetails): StubMapping = stubFor(
    get("/api/agencies/$agencyId")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(TestUtil.objectMapper().writeValueAsString(agencyDetails))
          .withStatus(200),
      ),
  )
}
