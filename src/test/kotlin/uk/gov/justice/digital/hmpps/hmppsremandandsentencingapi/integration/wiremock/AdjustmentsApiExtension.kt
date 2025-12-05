package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import java.util.*

class AdjustmentsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val adjustmentsApi = AdjustmentsApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext?) {
    adjustmentsApi.start()
  }

  override fun beforeEach(context: ExtensionContext?) {
    adjustmentsApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext?) {
    adjustmentsApi.stop()
  }
}

class AdjustmentsApiMockServer : WireMockServer(WireMockConfiguration.options().port(WIREMOCK_PORT).notifier(ConsoleNotifier(false))) {
  companion object {
    private const val WIREMOCK_PORT = 8552
    private val OBJECT_MAPPER: ObjectMapper = jacksonObjectMapper()
      .registerModule(JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  }

  fun stubGetRecallAdjustments(
    prisonerNumber: String,
    recallUuid: String,
    adjustments: List<AdjustmentDto>,
  ): StubMapping = stubFor(
    get(urlPathEqualTo("/adjustments"))
      .withQueryParam("person", equalTo(prisonerNumber))
      .withQueryParam("recallId", equalTo(recallUuid))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(OBJECT_MAPPER.writeValueAsString(adjustments))
          .withStatus(200),
      ),
  )

  fun stubGetPrisonerAdjustments(
    prisonerNumber: String,
    adjustments: List<AdjustmentDto>,
  ): StubMapping = stubFor(
    get(urlPathEqualTo("/adjustments"))
      .withQueryParam("person", equalTo(prisonerNumber))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(OBJECT_MAPPER.writeValueAsString(adjustments))
          .withStatus(200),
      ),
  )

  fun stubGetAdjustmentsDefaultToNone(): StubMapping = stubFor(
    get(urlPathEqualTo("/adjustments"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("[]")
          .withStatus(200),
      ),
  )

  fun stubDeleteAdjustment(adjustmentId: String): StubMapping = stubFor(
    delete("/adjustments/$adjustmentId")
      .willReturn(aResponse().withStatus(200)),
  )

  fun verifyAdjustmentDeleted(adjustmentId: String) {
    verify(deleteRequestedFor(urlPathEqualTo("/adjustments/$adjustmentId")))
  }

  fun stubAllowCreateAdjustments(): StubMapping = stubFor(
    post("/adjustments")
      .willReturn(
        aResponse().withStatus(201)
          .withBody(OBJECT_MAPPER.writeValueAsString(StubAdjustmentCreatedResponse(listOf(UUID.randomUUID())))),
      ),
  )

  fun verifyAdjustmentCreated(adjustment: AdjustmentDto) {
    verify(
      postRequestedFor(urlPathEqualTo("/adjustments")).withRequestBody(
        equalTo(
          OBJECT_MAPPER.writeValueAsString(listOf(adjustment)),
        ),
      ),
    )
  }

  fun stubAllowUpdateAdjustments(): StubMapping = stubFor(
    put(urlPathMatching("/adjustments/.+"))
      .willReturn(aResponse().withStatus(200)),
  )

  fun verifyAdjustmentUpdated(adjustmentId: String, updatedAdjustment: AdjustmentDto) {
    verify(
      putRequestedFor(urlPathEqualTo("/adjustments/$adjustmentId")).withRequestBody(
        equalTo(
          OBJECT_MAPPER.writeValueAsString(updatedAdjustment),
        ),
      ),
    )
  }

  fun verifyNoAdjustmentsCreated() {
    verify(
      0,
      postRequestedFor(urlPathEqualTo("/adjustments")),
    )
  }

  fun verifyNoAdjustmentsUpdated() {
    verify(
      0,
      putRequestedFor(urlPathMatching("/adjustments/.+")),
    )
  }

  fun verifyNoAdjustmentsDeleted() {
    verify(
      0,
      deleteRequestedFor(urlPathMatching("/adjustments/.+")),
    )
  }

  private data class StubAdjustmentCreatedResponse(val adjustmentIds: List<UUID>)
}
