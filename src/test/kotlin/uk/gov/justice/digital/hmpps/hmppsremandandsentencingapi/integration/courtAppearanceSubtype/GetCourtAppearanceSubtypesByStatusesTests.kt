package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtAppearanceSubtype

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceSubtypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceSubtypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyAppearanceTypeService
import java.util.UUID

class GetCourtAppearanceSubtypesByStatusesTests : IntegrationTestBase() {

  @Autowired
  private lateinit var appearanceTypeRepository: AppearanceTypeRepository

  @Autowired
  private lateinit var courtAppearanceSubtypeRepository: CourtAppearanceSubtypeRepository

  @Test
  fun `return all active types`() {
    webTestClient.get()
      .uri("/court-appearance-subtype/status?statuses=ACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.appearanceSubtypeUuid == '3f1c9e42-7c8a-4c1e-9a5d-2f6b8d1a9e73')]")
      .exists()
      .jsonPath("$.[?(@.appearanceSubtypeUuid == '8b7d2a91-5e3c-4f6f-8c2d-9a1b7e4c5d20')]")
      .exists()
      .jsonPath("$.[?(@.appearanceSubtypeUuid == 'c2a9f5d4-1e6b-4a9c-b8f7-3d2e1c6a9b84')]")
      .exists()
      .jsonPath("$.[?(@.appearanceSubtypeUuid == '5d9a3b7c-8e1f-4c2a-9b6d-7f3e2a1c8d55')]")
      .exists()
  }

  @Test
  fun `return all inactive types`() {
    val inactiveType = courtAppearanceSubtypeRepository.save(
      CourtAppearanceSubtypeEntity(
        appearanceSubtypeUuid = UUID.randomUUID(),
        description = "Inactive type",
        displayOrder = 1000,
        status = ReferenceEntityStatus.INACTIVE,
        nomisCode = "1111",
        appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(LegacyAppearanceTypeService.DEFAULT_APPEARANCE_TYPE_UUID)!!,
      ),
    )
    webTestClient.get()
      .uri("/court-appearance-subtype/status?statuses=INACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.appearanceSubtypeUuid == '${inactiveType.appearanceSubtypeUuid}')]")
      .exists()

    courtAppearanceSubtypeRepository.delete(inactiveType)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/court-appearance-subtype/status?statuses=ACTIVE")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
