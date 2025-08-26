package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID
import java.time.LocalDate
import java.util.UUID

class UpdateCourtCaseTests : IntegrationTestBase() {

  @Autowired
  private lateinit var periodLengthRepository: PeriodLengthRepository

  @Test
  fun `update court case`() {
    val courtCase = createCourtCase()
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = courtCase.second.appearances.first().appearanceUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE")
    val editedCourtCase = courtCase.second.copy(appearances = listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    val courtCaseLogs = courtCaseHistoryRepository.findAll().filter { it.prisonerId == courtCase.second.prisonerId }
    assertThat(courtCaseLogs).hasSize(2)
  }

  @Test
  fun `delete an appearance if emitted from list of appearances`() {
    val appearance = DpsDataCreator.dpsCreateCourtAppearance()
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(7))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance, secondAppearance))
    val courtCaseUuid = UUID.randomUUID()
    webTestClient
      .put()
      .uri("/court-case/$courtCaseUuid")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val courtCaseWithoutSecondAppearance = courtCase.copy(appearances = listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/$courtCaseUuid")
      .bodyValue(courtCaseWithoutSecondAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.latestAppearance.appearanceUuid")
      .isEqualTo(appearance.appearanceUuid.toString())
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${secondAppearance.appearanceUuid}')]")
      .doesNotExist()
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${appearance.appearanceUuid}')]")
      .exists()
  }

  @Test
  fun `cannot edit prisoner id of a court case`() {
    val courtCase = createCourtCase()
    val editedCourtCase = courtCase.second.copy(prisonerId = "ADIFFERENTPRISONER")

    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `new court case being put results in court case created event`() {
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(DpsDataCreator.dpsCreateCourtCase())
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val eventTypes = getMessages(7).map { it.eventType }
    Assertions.assertThat(eventTypes).contains("court-case.inserted")
  }

  @Test
  fun `no token results in unauthorized`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
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
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Associated Period lengths are deleted if the sentence is deleted`() {
    val courtCase = createCourtCase()
    val courtCaseUuid = courtCase.first
    val prisonerId = DEFAULT_PRISONER_ID
    val courtCaseBefore = getCourtCase(courtCaseUuid, prisonerId)

    // Check the period length is inserted and is active
    val periodLengthUuid = courtCaseBefore.appearances.first().charges.first().sentence?.periodLengths?.first()?.periodLengthUuid!!
    val periodLengthBefore = periodLengthRepository.findByPeriodLengthUuid(periodLengthUuid).first()
    assertThat(periodLengthBefore.statusId).isEqualTo(EntityStatus.ACTIVE)

    // Remove the first sentence then update the court case
    val editedCourtCase = courtCase.second.copy(
      appearances = courtCase.second.appearances.mapIndexed { index, appearance ->
        if (index == 0) {
          appearance.copy(
            charges = appearance.charges.mapIndexed { chargeIndex, charge ->
              if (chargeIndex == 0) {
                charge.copy(sentence = null)
              } else {
                charge
              }
            },
          )
        } else {
          appearance
        }
      },
    )
    updateCourtCase(courtCaseUuid, editedCourtCase)

    // Check the period length is now DELETED
    val periodLengthAfter = periodLengthRepository.findByPeriodLengthUuid(periodLengthUuid).first()
    assertThat(periodLengthAfter.statusId).isEqualTo(EntityStatus.DELETED)
  }

  private fun updateCourtCase(
    courtCaseUuid: String,
    editedCourtCase: CreateCourtCase,
  ) {
    webTestClient
      .put()
      .uri("/court-case/$courtCaseUuid")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun getCourtCase(
    courtCaseUuid: String,
    prisonerId: String,
  ): CourtCase = webTestClient
    .get()
    .uri { uriBuilder ->
      uriBuilder
        .path("/court-case/$courtCaseUuid")
        .queryParam("prisonerId", prisonerId)
        .build()
    }
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .returnResult(CourtCase::class.java)
    .responseBody.blockFirst()!!
}
