package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension
import java.time.LocalDate
import java.util.UUID

@Sql("classpath:test_data/reset-database.sql")
@ExtendWith(OAuthExtension::class, PrisonApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList()) {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "SOME_USER",
        roles = roles,
        client = "some-client",
        user = "SOME_USER",
      ),
    )
  }

  protected fun createCourtCase(prisonerId: String = "PRI123", minusDaysFromAppearanceDate: Long = 0): Pair<String, String> {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now().minusDays(minusDaysFromAppearanceDate), null, null, listOf(charge))
    val courtCase = CreateCourtCase(prisonerId, listOf(appearance))
    val response = webTestClient
      .post()
      .uri("/courtCase")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(CreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!
    return courtCase.prisonerId to response.courtCaseUuid
  }
}
