package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import java.util.UUID

class GetAllChargeOutcomesByStatusesTests : IntegrationTestBase() {

  @Autowired
  private lateinit var chargeOutcomeRepository: ChargeOutcomeRepository

  @Test
  fun `return all active outcomes`() {
    webTestClient.get()
      .uri("/charge-outcome/status?statuses=ACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.outcomeUuid == 'f17328cf-ceaa-43c2-930a-26cf74480e18')]")
      .exists()
      .jsonPath("$.[?(@.outcomeUuid == '315280e5-d53e-43b3-8ba6-44da25676ce2')]")
      .exists()
  }

  @Test
  fun `return all inactive outcomes`() {
    val inactiveOutcome = chargeOutcomeRepository.save(
      ChargeOutcomeEntity(
        outcomeName = "Inactive outcome",
        outcomeUuid = UUID.randomUUID(),
        nomisCode = "123456",
        outcomeType = "REMAND",
        displayOrder = 1000,
        dispositionCode = "F",
        status = ReferenceEntityStatus.INACTIVE,
      ),
    )
    webTestClient.get()
      .uri("/charge-outcome/status?statuses=INACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.outcomeUuid == '${inactiveOutcome.outcomeUuid}')]")
      .exists()
    chargeOutcomeRepository.delete(inactiveOutcome)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/charge-outcome/all")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
