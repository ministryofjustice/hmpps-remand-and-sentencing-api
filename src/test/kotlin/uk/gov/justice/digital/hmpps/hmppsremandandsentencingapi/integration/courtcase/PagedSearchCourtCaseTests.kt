package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.LongStream

class PagedSearchCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `return all court cases associated with a prisoner id`() {
    val createdCourtCase = createCourtCase()
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", createdCourtCase.second.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(createdCourtCase.first)
      .jsonPath("$.content.[*].prisonerId")
      .isEqualTo(createdCourtCase.second.prisonerId)
  }

  @Test
  fun `must only return court cases associated with prisoner id`() {
    val expectedCourtCase = createCourtCase()
    val otherCourtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = "OTHERPRISONER"))
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", expectedCourtCase.second.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(expectedCourtCase.first)
      .jsonPath("$.content.[*].prisonerId")
      .isEqualTo(expectedCourtCase.second.prisonerId)
      .jsonPath("$.content.[?(@.courtCaseUuid == '${otherCourtCase.first}')]")
      .doesNotExist()
      .jsonPath("$.content.[?(@.prisonerId == '${otherCourtCase.second.prisonerId}')]")
      .doesNotExist()
  }

  // The default size is 20
  @Test
  fun `return paged results and not all results`() {
    val courtCases = LongStream.range(0, 100).mapToObj { createCourtCase() }.toList()
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.first().second.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.length()")
      .isEqualTo(20)
      .jsonPath("$.totalElements")
      .isEqualTo(100)
  }

  @Test
  fun `sort by latest appearance date`() {
    val courtCases = LongStream.range(0, 5).mapToObj {
      createCourtCase(
        DpsDataCreator.dpsCreateCourtCase(
          appearances = listOf(
            DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(it)),
          ),
        ),
      )
    }.toList()
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.first().second.prisonerId)
          .queryParam("pagedCourtCaseOrderBy", "APPEARANCE_DATE_DESC")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(courtCases[0].first)
      .jsonPath("$.content.[1].courtCaseUuid")
      .isEqualTo(courtCases[1].first)
  }

  @Test
  fun `default sorting by status then appearance date descending`() {
    val courtCases = LongStream.range(0, 5).mapToObj {
      createCourtCase(
        DpsDataCreator.dpsCreateCourtCase(
          appearances = listOf(
            DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(it)),
          ),
        ),
      )
    }.toList()

    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.first().second.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(courtCases[0].first)
      .jsonPath("$.content.[1].courtCaseUuid")
      .isEqualTo(courtCases[1].first)
  }

  @Test
  fun `recalled court cases appear first when latest appearance is same date`() {
    val (courtCaseUuid) = createCourtCase()
    val (recalledCourtCaseUuid, recalledCourtCase) = createCourtCase()
    val toBeRecalledSentence = recalledCourtCase.appearances.first().charges.first().sentence!!
    adjustmentsApi.stubAllowCreateAdjustments()
    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(toBeRecalledSentence.sentenceUuid)))
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", recalledCourtCase.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(recalledCourtCaseUuid)
      .jsonPath("$.content.[1].courtCaseUuid")
      .isEqualTo(courtCaseUuid)
  }

  @Test
  fun `can filter out court cases based on latest appearance date from date`() {
    val appearanceDate = LocalDate.now()
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = appearanceDate))))
    val (pastCourtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = appearanceDate.minusDays(10)))))
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", createdCourtCase.prisonerId)
          .queryParam("appearanceDateFrom", appearanceDate.minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.totalElements")
      .isEqualTo(1)
      .jsonPath("$.content.[?(@.courtCaseUuid == '$courtCaseUuid')]")
      .exists()
      .jsonPath("$.content.[?(@.courtCaseUuid == '$pastCourtCaseUuid')]")
      .doesNotExist()
  }

  @Test
  fun `can filter out court cases based on latest appearance date to date`() {
    val appearanceDate = LocalDate.now()
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = appearanceDate))))
    val (pastCourtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = appearanceDate.minusDays(10)))))
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", createdCourtCase.prisonerId)
          .queryParam("appearanceDateTo", appearanceDate.minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.totalElements")
      .isEqualTo(1)
      .jsonPath("$.content.[?(@.courtCaseUuid == '$courtCaseUuid')]")
      .doesNotExist()
      .jsonPath("$.content.[?(@.courtCaseUuid == '$pastCourtCaseUuid')]")
      .exists()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", "PRISONER_ID")
          .build()
      }
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", "PRISONER_ID")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Court case search doesnt return any cases that have no appearances associated`() {
    val legacyCourtCase = createLegacyCourtCase()

    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", legacyCourtCase.second.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.totalElements")
      .isEqualTo(0)
  }
}
