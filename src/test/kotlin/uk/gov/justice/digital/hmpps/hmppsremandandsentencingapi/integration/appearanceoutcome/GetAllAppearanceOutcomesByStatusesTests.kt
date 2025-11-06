package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearanceoutcome

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import java.util.UUID

class GetAllAppearanceOutcomesByStatusesTests : IntegrationTestBase() {

  @Autowired
  private lateinit var appearanceOutcomeRepository: AppearanceOutcomeRepository

  @Test
  fun `return all active outcomes`() {
    webTestClient.get()
      .uri("/appearance-outcome/status?statuses=ACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.outcomeUuid == '62412083-9892-48c9-bf01-7864af4a8b3c')]")
      .exists()
      .jsonPath("$.[?(@.outcomeUuid == '2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8')]")
      .exists()
  }

  @Test
  fun `return all inactive outcomes`() {
    val inactiveOutcome = appearanceOutcomeRepository.save(
      AppearanceOutcomeEntity(
        outcomeName = "Inactive outcome",
        outcomeUuid = UUID.randomUUID(),
        nomisCode = "68734096",
        outcomeType = "REMAND",
        displayOrder = 1000,
        relatedChargeOutcomeUuid = UUID.randomUUID(),
        isSubList = false,
        status = ReferenceEntityStatus.INACTIVE,
        warrantType = "REMAND",
      ),
    )

    webTestClient.get()
      .uri("/appearance-outcome/status?statuses=INACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.outcomeUuid == '${inactiveOutcome.outcomeUuid}')]")
      .exists()

    appearanceOutcomeRepository.delete(inactiveOutcome)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/appearance-outcome/status?statuses=ACTIVE")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
