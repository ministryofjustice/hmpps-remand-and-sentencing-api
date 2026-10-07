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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison

class PersonRecordExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val personRecord = PersonRecordMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    personRecord.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    personRecord.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    personRecord.stop()
  }
}

class PersonRecordMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8554
  }

  fun stubGetPersonPrison(prisonerId: String, personPrison: PersonPrison): StubMapping = stubFor(
    get("/person/prison/$prisonerId")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(TestUtil.objectMapper().writeValueAsString(personPrison))
          .withStatus(200),
      ),
  )
}
