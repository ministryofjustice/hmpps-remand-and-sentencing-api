package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearancetype

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import java.util.UUID

class GetAllAppearanceTypesByStatusesTests : IntegrationTestBase() {

  @Autowired
  private lateinit var appearanceTypeRepository: AppearanceTypeRepository

  @Test
  fun `return all active types`() {
    webTestClient.get()
      .uri("/appearance-type/status?statuses=ACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.appearanceTypeUuid == '63e8fce0-033c-46ad-9edf-391b802d547a')]")
      .exists()
      .jsonPath("$.[?(@.appearanceTypeUuid == '1da09b6e-55cb-4838-a157-ee6944f2094c')]")
      .exists()
  }

  @Test
  fun `return all inactive types`() {
    val inactiveType = appearanceTypeRepository.save(
      AppearanceTypeEntity(
        appearanceTypeUuid = UUID.randomUUID(),
        description = "Inactive type",
        displayOrder = 1000,
        status = ReferenceEntityStatus.INACTIVE,
      ),
    )
    webTestClient.get()
      .uri("/appearance-type/status?statuses=INACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.appearanceTypeUuid == '${inactiveType.appearanceTypeUuid}')]")
      .exists()

    appearanceTypeRepository.delete(inactiveType)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/appearance-type/status?statuses=ACTIVE")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
