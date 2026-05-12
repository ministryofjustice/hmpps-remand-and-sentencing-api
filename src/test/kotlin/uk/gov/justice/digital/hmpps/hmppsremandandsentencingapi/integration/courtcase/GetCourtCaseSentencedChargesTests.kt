package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class GetCourtCaseSentencedChargesTests : IntegrationTestBase() {

  @Test
  fun `retrieve only the sentenced charges`() {
    val remandedCharge = DpsDataCreator.dpsCreateCharge(sentence = null, outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"))
    val remandedCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(remandedCharge), outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"))
    val sentencedCharge = DpsDataCreator.dpsCreateCharge()
    val sentencedCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(sentencedCharge))
    val remandedCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(remandedCourtAppearance, sentencedCourtAppearance))
    val (courtCaseUuid) = createCourtCase(remandedCourtCase)
    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid/sentenced-charges")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.chargeUuid == '${sentencedCharge.chargeUuid}')].offenceCode")
      .isEqualTo(sentencedCharge.offenceCode)
      .jsonPath("$.charges[?(@.chargeUuid == '${sentencedCharge.chargeUuid}')].outcome.outcomeUuid")
      .isEqualTo(sentencedCharge.outcomeUuid.toString())
      .jsonPath("$.charges[?(@.chargeUuid == '${remandedCharge.chargeUuid}')]")
      .doesNotExist()
  }

  @Test
  fun `no token results in unauthorized`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/sentenced-charges")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/sentenced-charges")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
