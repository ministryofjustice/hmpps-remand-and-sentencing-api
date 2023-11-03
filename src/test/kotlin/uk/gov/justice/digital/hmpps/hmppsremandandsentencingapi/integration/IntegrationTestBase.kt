package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension

@ExtendWith(OAuthExtension::class, PrisonApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient
}
