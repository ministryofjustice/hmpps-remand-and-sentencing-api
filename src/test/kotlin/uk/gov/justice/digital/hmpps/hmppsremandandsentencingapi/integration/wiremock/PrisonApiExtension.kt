package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext?) {
    prisonApi.start()
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
}
