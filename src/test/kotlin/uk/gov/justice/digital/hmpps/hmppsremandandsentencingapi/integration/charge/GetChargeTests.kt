package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.charge

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateCourtAppearance
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetChargeTests : IntegrationTestBase() {

  @Test
  fun `get charge by uuid`() {
    val createdCharge = createCourtCase().second.appearances.first().charges.first()
    webTestClient
      .get()
      .uri("/charge/${createdCharge.chargeUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeUuid")
      .isEqualTo(createdCharge.chargeUuid.toString())
      .jsonPath("$.offenceCode")
      .isEqualTo(createdCharge.offenceCode)
      .jsonPath("$.offenceStartDate")
      .isEqualTo(createdCharge.offenceStartDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.outcome.outcomeUuid")
      .isEqualTo(createdCharge.outcomeUuid!!.toString())
      .jsonPath("$.sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo(createdCharge.sentence!!.sentenceTypeId.toString())
  }

  @Test
  fun `no charge exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdCharge = createCourtCase().second.appearances.first().charges.first()
    webTestClient
      .get()
      .uri("/charge/${createdCharge.chargeUuid!!}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCharge = createCourtCase().second.appearances.first().charges.first()
    webTestClient
      .get()
      .uri("/charge/${createdCharge.chargeUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get charge with terrorRelated true returns OATC in aggravatingFactors`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(terrorRelated = true, foreignPowerRelated = null, sentence = null)
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(dpsCreateCourtAppearance(charges = listOf(createCharge)))))

    webTestClient
      .get()
      .uri("/charge/${createCharge.chargeUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.aggravatingFactors").isArray
      .jsonPath("$.aggravatingFactors.length()").isEqualTo(1)
      .jsonPath("$.aggravatingFactors[0].code").isEqualTo("OATC")
  }

  @Test
  fun `should get charge with foreignPowerRelated true returns OAFPC in aggravatingFactors`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(terrorRelated = null, foreignPowerRelated = true, sentence = null)
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(dpsCreateCourtAppearance(charges = listOf(createCharge)))))

    webTestClient
      .get()
      .uri("/charge/${createCharge.chargeUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.aggravatingFactors").isArray
      .jsonPath("$.aggravatingFactors.length()").isEqualTo(1)
      .jsonPath("$.aggravatingFactors[0].code").isEqualTo("OAFPC")
  }

  @Test
  fun `should get charge with both flags true returns both aggravating factors`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(terrorRelated = true, foreignPowerRelated = true, sentence = null)
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(dpsCreateCourtAppearance(charges = listOf(createCharge)))))

    webTestClient
      .get()
      .uri("/charge/${createCharge.chargeUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.aggravatingFactors").isArray
      .jsonPath("$.aggravatingFactors.length()").isEqualTo(2)
      .jsonPath("$.aggravatingFactors[*].code").value<List<String>> {
        assertThat(it).containsExactlyInAnyOrder("OATC", "OAFPC")
      }
  }

  @Test
  fun `should get charge with no flags set returns empty aggravatingFactors`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(terrorRelated = null, foreignPowerRelated = null, sentence = null)
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(dpsCreateCourtAppearance(charges = listOf(createCharge)))))

    webTestClient
      .get()
      .uri("/charge/${createCharge.chargeUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.aggravatingFactors").isArray
      .jsonPath("$.aggravatingFactors.length()").isEqualTo(0)
  }
}
