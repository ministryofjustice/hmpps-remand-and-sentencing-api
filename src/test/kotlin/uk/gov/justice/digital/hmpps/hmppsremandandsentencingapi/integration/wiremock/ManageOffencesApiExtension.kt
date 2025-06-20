package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ManageOffencesApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val manageOffencesApi = ManageOffencesApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext?) {
    manageOffencesApi.start()
    manageOffencesApi.stubGetOffencesByCode()
  }

  override fun beforeEach(context: ExtensionContext?) {
    manageOffencesApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext?) {
    manageOffencesApi.stop()
  }
}

class ManageOffencesApiMockServer : WireMockServer(WireMockConfiguration.options().port(WIREMOCK_PORT).notifier(ConsoleNotifier(false))) {
  companion object {
    private const val WIREMOCK_PORT = 8443
  }

  fun stubGetOffencesByCode(): StubMapping = stubFor(
    get(urlPathEqualTo("/offences/code/multiple"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            [
              {
                "id": 1,
                "code": "THEFT001",
                "description": "Theft from a shop",
                "offenceType": "SUMMARY",
                "revisionId": 1,
                "startDate": "2020-01-01",
                "endDate": null
              },
              {
                "id": 2,
                "code": "ASSAULT001",
                "description": "Common assault",
                "offenceType": "EITHER_WAY",
                "revisionId": 1,
                "startDate": "2020-01-01",
                "endDate": null
              }
            ]
            """.trimIndent(),
          ),
      ),
  )

  fun stubGetOffencesByCodeWithSpecificCodes(vararg codes: String): StubMapping {
    val codesParam = codes.joinToString(",")
    return stubFor(
      get(urlPathEqualTo("/offences/code/multiple"))
        .withQueryParam("offenceCodes", equalTo(codesParam))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              codes.mapIndexed { index, code ->
                """
                {
                  "id": ${index + 1},
                  "code": "$code",
                  "description": "Description for $code",
                  "offenceType": "SUMMARY",
                  "revisionId": 1,
                  "startDate": "2020-01-01",
                  "endDate": null
                }
                """.trimIndent()
              }.joinToString(",", "[", "]"),
            ),
        ),
    )
  }

  fun stubGetOffencesByCodeEmptyResponse(): StubMapping = stubFor(
    get(urlPathEqualTo("/offences/code/multiple"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody("[]"),
      ),
  )

  fun stubGetOffencesByCodeError(): StubMapping = stubFor(
    get(urlPathEqualTo("/offences/code/multiple"))
      .willReturn(
        aResponse()
          .withStatus(500)
          .withHeader("Content-Type", "application/json")
          .withBody("""{"error": "Internal server error"}"""),
      ),
  )
}