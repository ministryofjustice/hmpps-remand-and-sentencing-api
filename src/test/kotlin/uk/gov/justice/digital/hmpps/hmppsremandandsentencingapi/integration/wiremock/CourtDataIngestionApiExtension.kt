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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing

class CourtDataIngestionApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val courtDataIngestionApi = CourtDataIngestionApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    courtDataIngestionApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    courtDataIngestionApi.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    courtDataIngestionApi.stop()
  }
}

class CourtDataIngestionApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8553
  }

  fun stubCourtHearing(courtHearing: HmctsCourHearing): StubMapping = stubFor(
    get("/court-hearings/${courtHearing.hearingId}")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            TestUtil.objectMapper().writeValueAsString(courtHearing),
          )
          .withStatus(200),
      ),
  )

  fun stubCourtHearingsByPrisoner(prisonerId: String, courtHearings: List<HmctsCourHearing>): StubMapping = stubFor(
    get("/court-hearings/prisoner/$prisonerId")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            TestUtil.objectMapper().writeValueAsString(courtHearings),
          )
          .withStatus(200),
      ),
  )
}
