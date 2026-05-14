package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi.documentMetadataRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension.Companion.documentManagementApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class CreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `Successfully create court case`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value<String> { courtCaseUuid ->
        Assertions.assertThat(courtCaseUuid).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")
      }
      .jsonPath("$.appearances[0].appearanceUuid")
      .isEqualTo(createCourtCase.appearances.first().appearanceUuid.toString())
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(createCourtCase.appearances.first().charges.first().chargeUuid.toString())
    expectInsertedMessages(createCourtCase.prisonerId)

    val courtCaseLogs = courtCaseHistoryRepository.findAll().filter { it.prisonerId == createCourtCase.prisonerId }
    assertThat(courtCaseLogs).hasSize(1)
  }

  @Test
  fun `Successfully create court case and link document`() {
    val (uploadedDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentMetadata(uploadedDocument.documentUUID.toString())

    val createCourtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(DpsDataCreator.dpsCreateCourtAppearance(documents = listOf(uploadedDocument)))
    )

    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value<String> { courtCaseUuid ->
        Assertions.assertThat(courtCaseUuid).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")
      }

    documentManagementApi.verify(
      WireMock.putRequestedFor(WireMock.urlEqualTo("/documents/${uploadedDocument.documentUUID}/metadata"))
        .withRequestBody(
          WireMock.equalToJson(
            documentMetadataRequest(
              createCourtCase.prisonerId,
              "Active",
            ),
          ),
        ),
    )
  }

  @Test
  fun `Create court case and link document finishes even if document management api fails`() {
    val (uploadedDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentMetadataToFail(uploadedDocument.documentUUID.toString())

    val createCourtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(DpsDataCreator.dpsCreateCourtAppearance(documents = listOf(uploadedDocument)))
    )

    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value<String> { courtCaseUuid ->
        Assertions.assertThat(courtCaseUuid).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")
      }

    documentManagementApi.verify(
      WireMock.putRequestedFor(WireMock.urlEqualTo("/documents/${uploadedDocument.documentUUID}/metadata"))
        .withRequestBody(
          WireMock.equalToJson(
            documentMetadataRequest(
              createCourtCase.prisonerId,
              "Active",
            ),
          ),
        ),
    )
  }

  @Test
  fun `no token results in unauthorized`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
