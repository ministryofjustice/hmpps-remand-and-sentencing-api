package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sync

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity

class AppearanceChargeNotLinkTests : IntegrationTestBase() {

  /*
    This happens when the charge is associated to multiple court appearances, gets unlinked
    to all appearances at the same time which keeps it ACTIVE. The call to link the charge again
    to a new appearance fails to create the link to the new appearance. Even simulating this won't
    be deterministic because the multiple calls to unlink may process in such a way as it would
    set the charge to DELETE so this test inserts directly into the db instead
   */
  @Test
  fun `no link created for charge after multiple unlinks called at the same time`() {
    val legacyCreateCharge = DataCreator.legacyCreateCharge(
      legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null, outcomeConvictionFlag = null),
    )
    val charge = chargeRepository.save(ChargeEntity.from(legacyCreateCharge, null, "USER"))

    val (appearanceUuid, legacyCourtAppearance) = createLegacyCourtAppearance()

    val insertChargeInAppearance = DataCreator.legacyUpdateCharge(offenceStartDate = legacyCreateCharge.offenceStartDate, offenceEndDate = legacyCreateCharge.offenceEndDate, legacyData = DataCreator.chargeLegacyData(), performedByUser = charge.createdBy)

    webTestClient
      .put()
      .uri("/legacy/court-appearance/$appearanceUuid/charge/${charge.chargeUuid}")
      .bodyValue(insertChargeInAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/legacy/court-case/${legacyCourtAppearance.courtCaseUuid}/reconciliation")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$appearanceUuid')].charges[?(@.chargeUuid == '${charge.chargeUuid}')].nomisOutcomeCode")
      .isEqualTo(insertChargeInAppearance.legacyData.nomisOutcomeCode)
  }
}
