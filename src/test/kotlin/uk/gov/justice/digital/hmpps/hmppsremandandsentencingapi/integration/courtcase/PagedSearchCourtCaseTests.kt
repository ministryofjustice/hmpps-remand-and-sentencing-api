package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateCourtAppearance
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.stream.LongStream

class PagedSearchCourtCaseTests : IntegrationTestBase() {

  @ParameterizedTest
  @ValueSource(
    strings = [
      "ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI",
      "ROLE_REMAND_AND_SENTENCING__CCRD__RO",
      "ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI,ROLE_REMAND_AND_SENTENCING__CCRD__RO",
    ],
  )
  fun `return all court cases associated with a prisoner id`(roleCsv: String) {
    val roles = roleCsv.split(",")
    val createdCourtCase = createCourtCase()
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", createdCourtCase.second.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = roles)
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
      .jsonPath("$.content.[0].canAppeal")
      .isEqualTo(false)
      .jsonPath("$.content.[0].canBreach")
      .isEqualTo(false)
      .jsonPath("$.content.[1].courtCaseUuid")
      .isEqualTo(courtCaseUuid)
      .jsonPath("$.content.[1].canAppeal")
      .isEqualTo(true)
      .jsonPath("$.content.[1].canBreach")
      .isEqualTo(false)
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
      .jsonPath("$.prisonerCourtCaseTotal")
      .isEqualTo(2)
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
      .jsonPath("$.prisonerCourtCaseTotal")
      .isEqualTo(2)
  }

  @Test
  fun `can filter by booking id`() {
    val bookingId = 1L
    val courtCaseInBooking = DataCreator.migrationCreateCourtCase(courtCaseLegacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId))
    val courtCaseInOtherBooking = DataCreator.migrationCreateCourtCase(caseId = 2, courtCaseLegacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId + 1))
    val migrateCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCaseInBooking, courtCaseInOtherBooking))
    val response = migrateCases(migrateCourtCases)
    val courtCaseUuidInBooking = response.courtCases.first { it.caseId == courtCaseInBooking.caseId }.courtCaseUuid
    val courtCaseUuidInOtherBooking = response.courtCases.first { it.caseId == courtCaseInOtherBooking.caseId }.courtCaseUuid
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", migrateCourtCases.prisonerId)
          .queryParam("bookingId", bookingId)
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
      .jsonPath("$.content.[?(@.courtCaseUuid == '$courtCaseUuidInBooking')]")
      .exists()
      .jsonPath("$.content.[?(@.courtCaseUuid == '$courtCaseUuidInOtherBooking')]")
      .doesNotExist()
  }

  @Test
  fun `many charges to a single sentence executes once on multiple calls`() {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1111, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))
    migrateCases(courtCases)
    val firstCall = CompletableFuture.supplyAsync {
      webTestClient
        .get()
        .uri {
          it.path("/court-case/paged/search")
            .queryParam("prisonerId", courtCases.prisonerId)
            .build()
        }
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        }
        .exchange()
        .expectStatus()
        .isOk
    }
    val secondCall = CompletableFuture.supplyAsync {
      webTestClient
        .get()
        .uri {
          it.path("/court-case/paged/search")
            .queryParam("prisonerId", courtCases.prisonerId)
            .build()
        }
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        }
        .exchange()
        .expectStatus()
        .isOk
    }
    firstCall.thenCombine(secondCall) { a, b -> a to b }.join()
    val messages = getMessages(3)
    Assertions.assertThat(messages.map { it.eventType }).containsExactlyInAnyOrder("sentence.fix-single-charge.inserted", "sentence.updated", "sentence.period-length.inserted")
    Assertions.assertThat(messages).extracting<String> { it.personReference.identifiers.first().value }.containsOnly(courtCases.prisonerId)
  }

  @Test
  fun `keep single breach of supervision term length on retained sentence and remove from the rest`() {
    val termLength = DataCreator.migrationCreatePeriodLength(periodLengthId = DataCreator.nomisPeriodLengthId(termSequence = 1), legacyData = DataCreator.periodLengthLegacyData(sentenceTermCode = "IMP"))
    val breachOfSupervisionTermLength = DataCreator.migrationCreatePeriodLength(periodLengthId = DataCreator.nomisPeriodLengthId(termSequence = 2), legacyData = DataCreator.periodLengthLegacyData(sentenceTermCode = "SEC104"))
    val sentence = DataCreator.migrationCreateSentence(periodLengths = listOf(termLength, breachOfSupervisionTermLength))
    val firstCharge = DataCreator.migrationCreateCharge(sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1111, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))
    val response = migrateCases(courtCases)
    val breachOfSupervisionUuid = response.sentenceTerms.first { it.sentenceTermNOMISId == breachOfSupervisionTermLength.periodLengthId }.periodLengthUuid
    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
    val periodLengthRecords = periodLengthRepository.findByPeriodLengthUuid(breachOfSupervisionUuid)
    Assertions.assertThat(periodLengthRecords).extracting<PeriodLengthEntityStatus> { it.statusId }.containsExactlyInAnyOrder(
      PeriodLengthEntityStatus.ACTIVE,
      PeriodLengthEntityStatus.DELETED,
    )
  }

  @Test
  fun `can breach when there is a DTO sentence on the court case`() {
    val dtoSentence = DpsDataCreator.dpsCreateSentence(
      sentenceTypeId = UUID.fromString("903ca33b-e264-4a16-883d-fee03a2a3396"),
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(type = PeriodLengthType.TERM_LENGTH),
      ),
    )
    val dtoCharge = DpsDataCreator.dpsCreateCharge(outcomeUuid = UUID.fromString("0460ad51-04ea-402a-a249-b152b052a385"), sentence = dtoSentence)
    val dtoAppearance = dpsCreateCourtAppearance(outcomeUuid = UUID.fromString("bcc438da-b3b4-4ca8-a870-9d17543e4317"), charges = listOf(dtoCharge))
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(dtoAppearance)))
    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", createdCourtCase.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].canBreach")
      .isEqualTo(true)
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

  @Test
  fun `return court cases along with aggravating factors`() {
    val charge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(code = "DISV", title = "Disability of victim", description = "Disability of victim", displayOrder = 120),
      ),
    )
    val createdCourtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(dpsCreateCourtAppearance(charges = listOf(charge)))))

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
      .jsonPath("$.content.[*].latestCourtAppearance.charges.[*].aggravatingFactors.[0].code")
      .isEqualTo("DISV")
  }

  @Test
  fun `return breach of supervision appearance period length`() {
    val sentencedCharge = DpsDataCreator.dpsCreateCharge()
    val sentencingAppearance = dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(10), nextCourtAppearance = null, charges = listOf(sentencedCharge))
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencingAppearance)))
    val breachPeriodLength = DpsDataCreator.dpsCreatePeriodLength(type = PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS, days = 41, years = null)
    val breachAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      warrantType = "BREACH_OF_SUPERVISION_REQUIREMENTS",
      overallSentenceLength = null,
      nextCourtAppearance = null,
      charges = listOf(
        sentencedCharge.copy(sentence = null),
      ),
      periodLengths = listOf(breachPeriodLength),
    )
    putCourtAppearance(breachAppearance.appearanceUuid, breachAppearance)
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", createdCourtCase.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].overallSentenceLength")
      .doesNotExist()
      .jsonPath("$.content.[0].latestCourtAppearance.periodLengths[?(@.periodLengthUuid == '${breachPeriodLength.periodLengthUuid}')]")
      .exists()
  }
}
