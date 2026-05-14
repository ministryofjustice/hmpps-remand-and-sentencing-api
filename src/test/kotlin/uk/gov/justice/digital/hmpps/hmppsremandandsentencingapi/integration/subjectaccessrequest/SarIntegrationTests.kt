package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util.ExpectResponseData.emptyNotInNomisResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate

/**
 * To check sar-generated-report.pdf, execute below and check the build/test-generated folder
 * ```bash
 * SAR_GENERATE_ACTUAL=true ./gradlew test \
 *  --tests "uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.SarIntegrationTests.SAR report should render as expected"
 * ```
 */
@Import(SarIntegrationTestHelperConfig::class)
@ActiveProfiles("test", "test-sar")
@DisplayName("SarIntegrationTests (Unsynced Data)")
class SarIntegrationTests :
  IntegrationTestBase(),
  SarApiDataTest,
  SarReportTest {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  /**
   * Generate the PDF to check as needed
   */
  @Test
  @EnabledIfEnvironmentVariable(named = "SAR_GENERATE_ACTUAL", matches = "true")
  override fun `SAR report should render as expected`() {
    arrange()
    super<SarReportTest>.`SAR report should render as expected`()
  }

  /**
   * Compares /subject-access-request API response
   * with expected JSON defined in hmpps.sar.tests.expected-api-response.path
   */
  @Test
  override fun `SAR API should return expected data`() {
    arrange()
    super<SarApiDataTest>.`SAR API should return expected data`()
  }

  @Test
  fun `SAR API should get empty immigrationDetentions by valid prisoner id with no data yet associated`() {
    createCourtCaseTwoSentences()
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "PRI123")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(emptyNotInNomisResponse())
  }

  @Test
  fun `SAR API should get 209 by attempting use of crn`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("crn", "foo-bar")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(209)
  }

  @Test
  fun `SAR API should get 204 when no data returned from service`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(204)
  }

  @Test
  fun `SAR API should get 400 when query not properly formed`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(400)
  }

  @Test
  fun `SAR API should get 401 when auth token missing or not valid`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .exchange()
      .expectStatus()
      .isEqualTo(401)
  }

  @Test
  fun `SAR API should get 403 when auth token role not SAR_DATA_ACCESS`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_LIMITED_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(403)
  }

  @Nested
  @DisplayName("Internal Server Errors")
  inner class ErrorScenarios {

    @MockitoBean
    private lateinit var prisonerDetailsService: PrisonerDetailsService

    @Test
    fun `SAR API should get 500 when internal exception thrown`() {
      whenever(
        prisonerDetailsService
          .getPrisonerDetails("foo-bar", null, null),
      ).thenThrow(RuntimeException::class.java)
      webTestClient
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/subject-access-request")
            .queryParam("prn", "foo-bar")
            .build()
        }
        .headers {
          it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
        }
        .exchange()
        .expectStatus()
        .isEqualTo(500)
    }
  }

  override fun setupTestData() {}
  override fun getPrn(): String? = DpsDataCreator.DEFAULT_PRISONER_ID
  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper
  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  private fun arrange() {
    createCourtCaseTwoSentences()
    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2021, 1, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
        noLongerOfInterestReason = ImmigrationDetentionNoLongerOfInterestType.RIGHT_TO_REMAIN,
        noLongerOfInterestComment = "Currently has the right to Remain",
        homeOfficeReferenceNumber = "3240593452",
      ),
    )

    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2025, 2, 2),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
        noLongerOfInterestReason = ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN,
        noLongerOfInterestComment = "Recently made a British Citizen",
        homeOfficeReferenceNumber = "5340593452",
      ),
    )
  }
}
