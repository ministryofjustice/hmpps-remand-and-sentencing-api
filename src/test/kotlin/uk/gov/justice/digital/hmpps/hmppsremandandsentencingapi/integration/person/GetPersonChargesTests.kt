package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateCourtCaseStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.charges.PersonCharges
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetPersonChargesTests : IntegrationTestBase() {

  @Test
  fun `retrieve all charges for a person, no query parameters means active only`() {
    val prisonerId = DEFAULT_PRISONER_ID
    val (_, createdCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val createdCharge = createdCourtCase.appearances.first().charges.first()
    val (inactiveCourtCaseUuid, createdInactiveCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val inactiveCharge = createdInactiveCourtCase.appearances.first().charges.first()
    webTestClient
      .put()
      .uri("/court-case/$inactiveCourtCaseUuid/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "No longer required"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/person/$prisonerId/charges")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')]"))
      .exists()
      .jsonPath(("$.charges[?(@.chargeUuid == '${inactiveCharge.chargeUuid}')]"))
      .doesNotExist()
  }

  @Test
  fun `can return inactive charges for a person`() {
    val prisonerId = DEFAULT_PRISONER_ID
    val (_, createdCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val createdCharge = createdCourtCase.appearances.first().charges.first()
    val (inactiveCourtCaseUuid, createdInactiveCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val inactiveCharge = createdInactiveCourtCase.appearances.first().charges.first()
    webTestClient
      .put()
      .uri("/court-case/$inactiveCourtCaseUuid/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "No longer required"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri {
        it.path("/person/$prisonerId/charges").queryParam("statuses", "INACTIVE")
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')]"))
      .doesNotExist()
      .jsonPath(("$.charges[?(@.chargeUuid == '${inactiveCharge.chargeUuid}')]"))
      .exists()
  }

  @Test
  fun `can filter using from date`() {
    val prisonerId = DEFAULT_PRISONER_ID
    val fromDate = LocalDate.now().minusDays(10)
    val (_, beforeCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = fromDate.minusDays(10), nextCourtAppearance = null),
        ),
      ),
    )
    val beforeFromDateCharge = beforeCourtCase.appearances.first().charges.first()
    val (_, afterCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = fromDate.plusDays(2), nextCourtAppearance = null),
        ),
      ),
    )
    val afterFromDateCharge = afterCourtCase.appearances.first().charges.first()
    webTestClient
      .get()
      .uri {
        it.path("/person/$prisonerId/charges")
          .queryParam("fromDate", fromDate.format(DateTimeFormatter.ISO_DATE))
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${beforeFromDateCharge.chargeUuid}')]"))
      .doesNotExist()
      .jsonPath(("$.charges[?(@.chargeUuid == '${afterFromDateCharge.chargeUuid}')]"))
      .exists()
  }

  @Test
  fun `can filter using to date`() {
    val prisonerId = DEFAULT_PRISONER_ID
    val toDate = LocalDate.now().minusDays(10)
    val (_, beforeCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = toDate.minusDays(10), nextCourtAppearance = null),
        ),
      ),
    )
    val beforeToDateCharge = beforeCourtCase.appearances.first().charges.first()
    val (_, afterCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = toDate.plusDays(2), nextCourtAppearance = null),
        ),
      ),
    )
    val afterToDateCharge = afterCourtCase.appearances.first().charges.first()
    webTestClient
      .get()
      .uri {
        it.path("/person/$prisonerId/charges")
          .queryParam("toDate", toDate.format(DateTimeFormatter.ISO_DATE))
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${beforeToDateCharge.chargeUuid}')]"))
      .exists()
      .jsonPath(("$.charges[?(@.chargeUuid == '${afterToDateCharge.chargeUuid}')]"))
      .doesNotExist()
  }

  @Nested
  inner class RecallTypeTests {

    @BeforeEach
    fun setUp() {
      adjustmentsApi.stubAllowCreateAdjustments()
      adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    }

    @Test
    fun `should retrieve all charges and latest recall type per sentence for a person with a single recall`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
      val originalRecall = DpsDataCreator.dpsCreateRecall(
        sentenceIds = listOf(
          sentenceOne.sentenceUuid,
          sentenceTwo.sentenceUuid,
        ),
      )
      createRecall(originalRecall)
      purgeQueues()
      sleep(1000)

      webTestClient
        .get()
        .uri("/person/$prisonerId/charges")
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.charges[?(@.sentence.sentenceUuid == '${sentenceOne.sentenceUuid}')].sentence.latestRecallType")
        .isEqualTo(listOf(RecallType.FTR_14.name))
        .jsonPath("$.charges[?(@.sentence.sentenceUuid == '${sentenceTwo.sentenceUuid}')].sentence.latestRecallType")
        .isEqualTo(listOf(RecallType.FTR_14.name))
    }

    @Test
    fun `should retrieve all charges and latest recall type per sentence for a person with multiple recalls`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
      val originalRecall = DpsDataCreator.dpsCreateRecall(
        sentenceIds = listOf(
          sentenceOne.sentenceUuid,
          sentenceTwo.sentenceUuid,
        ),
      )
      createRecall(originalRecall)
      purgeQueues()
      sleep(1000)

      val recallTwo = DpsDataCreator.dpsCreateRecall(
        recallTypeCode = RecallType.FTR_56,
        sentenceIds = listOf(
          sentenceOne.sentenceUuid,
        ),
        revocationDate = originalRecall.revocationDate.plusWeeks(4),
        returnToCustodyDate = originalRecall.returnToCustodyDate!!.plusWeeks(4),
      )
      createRecall(recallTwo)

      webTestClient
        .get()
        .uri("/person/$prisonerId/charges")
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.charges[?(@.sentence.sentenceUuid == '${sentenceOne.sentenceUuid}')].sentence.latestRecallType")
        .isEqualTo(listOf(RecallType.FTR_56.name))
        .jsonPath("$.charges[?(@.sentence.sentenceUuid == '${sentenceTwo.sentenceUuid}')].sentence.latestRecallType")
        .isEqualTo(listOf(RecallType.FTR_14.name))
    }

    @Test
    fun `should retrieve all charges and no recall type per sentence for a person with no recalls`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()

      webTestClient
        .get()
        .uri("/person/$prisonerId/charges")
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.charges[?(@.sentence.sentenceUuid == '${sentenceOne.sentenceUuid}')].sentence.latestRecallType")
        .isEqualTo(null)
        .jsonPath("$.charges[?(@.sentence.sentenceUuid == '${sentenceTwo.sentenceUuid}')].sentence.latestRecallType")
        .isEqualTo(null)
    }
  }

  @Nested
  inner class WarrantTests {

    @Test
    fun `should return the warrant date - created at for a non deleted appearance`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val appearanceDate = LocalDate.now().minusDays(5)
      val (_, createdCourtCase) = createCourtCase(
        createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
          prisonerId = prisonerId,
          appearances = listOf(
            DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = appearanceDate, nextCourtAppearance = null),
          ),
        ),
      )
      val charge = createdCourtCase.appearances.first().charges.first()

      webTestClient
        .get()
        .uri("/person/$prisonerId/charges")
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].warrantDate")
        .isEqualTo(listOf(appearanceDate.format(DateTimeFormatter.ISO_DATE)))
        .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].warrantCreatedAt")
        .isNotEmpty
    }

    @Test
    fun `should return a record with the warrant date - created at for each appearance when a charge is on multiple appearances`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val chargeUuid = UUID.randomUUID()
      val earlierDate = LocalDate.now().minusDays(20)
      val latestDate = LocalDate.now().minusDays(5)
      createCourtCase(
        createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
          prisonerId = prisonerId,
          appearances = listOf(
            DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
              appearanceDate = earlierDate,
              nextCourtAppearance = null,
              charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge(chargeUuid = chargeUuid)),
            ),
            DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
              appearanceDate = latestDate,
              nextCourtAppearance = null,
              charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge(chargeUuid = chargeUuid)),
            ),
          ),
        ),
      )

      val charges = webTestClient
        .get()
        .uri("/person/$prisonerId/charges")
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(PersonCharges::class.java)
        .returnResult()
        .responseBody!!
        .charges
        .filter { it.chargeUuid == chargeUuid }

      assertThat(charges.map { it.warrantDate }).containsExactlyInAnyOrder(earlierDate, latestDate)
    }

    @Test
    fun `should return the warrant date - created at for each active appearance across multiple court cases`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val appearanceDates = listOf(
        LocalDate.now().minusDays(21),
        LocalDate.now().minusDays(14),
        LocalDate.now().minusDays(7),
      )
      val chargesWithDates = appearanceDates.mapIndexed { index, appearanceDate ->
        val (_, createdCourtCase) = createCourtCase(
          createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
            prisonerId = prisonerId,
            appearances = listOf(
              DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
                appearanceDate = appearanceDate,
                nextCourtAppearance = null,
                charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge(offenceCode = "OFF12$index")),
              ),
            ),
          ),
        )
        createdCourtCase.appearances.first().charges.first() to appearanceDate
      }

      val response = webTestClient
        .get()
        .uri("/person/$prisonerId/charges")
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()

      chargesWithDates.forEach { (charge, appearanceDate) ->
        response
          .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].warrantDate")
          .isEqualTo(listOf(appearanceDate.format(DateTimeFormatter.ISO_DATE)))
          .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].warrantCreatedAt")
          .isNotEmpty
      }
    }

    @Test
    fun `should return the warrant date - created at for each non deleted non active appearance across multiple court cases`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val appearanceDates = listOf(
        LocalDate.now().minusDays(21),
        LocalDate.now().minusDays(14),
        LocalDate.now().minusDays(7),
      )
      val chargesWithDates = appearanceDates.mapIndexed { index, appearanceDate ->
        val (courtCaseUuid, createdCourtCase) = createCourtCase(
          createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
            prisonerId = prisonerId,
            appearances = listOf(
              DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
                appearanceDate = appearanceDate,
                nextCourtAppearance = null,
                charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge(offenceCode = "OFF12$index")),
              ),
            ),
          ),
        )
        webTestClient
          .put()
          .uri("/court-case/$courtCaseUuid/status")
          .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Making inactive for this test"))
          .headers {
            it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
            it.contentType = MediaType.APPLICATION_JSON
          }
          .exchange()
          .expectStatus()
          .isNoContent
        createdCourtCase.appearances.first().charges.first() to appearanceDate
      }

      val response = webTestClient
        .get()
        .uri { it.path("/person/$prisonerId/charges").queryParam("statuses", "INACTIVE").build() }
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()

      chargesWithDates.forEach { (charge, appearanceDate) ->
        response
          .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].warrantDate")
          .isEqualTo(listOf(appearanceDate.format(DateTimeFormatter.ISO_DATE)))
          .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].warrantCreatedAt")
          .isNotEmpty
      }
    }

    @Test
    fun `should return the warrant date - created at only for appearances within the from and to dates`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val chargeUuid = UUID.randomUUID()
      val beforeRangeDate = LocalDate.now().minusDays(30)
      val inRangeDate = LocalDate.now().minusDays(20)
      val afterRangeDate = LocalDate.now().minusDays(5)
      createCourtCase(
        createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
          prisonerId = prisonerId,
          appearances = listOf(beforeRangeDate, inRangeDate, afterRangeDate).map {
            DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
              appearanceDate = it,
              nextCourtAppearance = null,
              charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge(chargeUuid = chargeUuid)),
            )
          },
        ),
      )

      webTestClient
        .get()
        .uri {
          it.path("/person/$prisonerId/charges")
            .queryParam("fromDate", LocalDate.now().minusDays(25).format(DateTimeFormatter.ISO_DATE))
            .queryParam("toDate", LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_DATE))
            .build()
        }
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.charges[?(@.chargeUuid == '$chargeUuid')].warrantDate")
        .isEqualTo(listOf(inRangeDate.format(DateTimeFormatter.ISO_DATE)))
        .jsonPath("$.charges[?(@.chargeUuid == '$chargeUuid')].warrantCreatedAt")
        .isNotEmpty
    }

    @Test
    fun `should not return charges when the from and to dates out of range`() {
      val prisonerId = DEFAULT_PRISONER_ID
      val chargeUuid = UUID.randomUUID()
      val beforeRangeDate = LocalDate.now().minusDays(30)
      val inRangeDate = LocalDate.now().minusDays(20)
      val afterRangeDate = LocalDate.now().minusDays(5)
      createCourtCase(
        createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
          prisonerId = prisonerId,
          appearances = listOf(beforeRangeDate, inRangeDate, afterRangeDate).map {
            DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
              appearanceDate = it,
              nextCourtAppearance = null,
              charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge(chargeUuid = chargeUuid)),
            )
          },
        ),
      )

      webTestClient
        .get()
        .uri {
          it.path("/person/$prisonerId/charges")
            .queryParam("fromDate", LocalDate.now().minusDays(40).format(DateTimeFormatter.ISO_DATE))
            .queryParam("toDate", LocalDate.now().minusDays(35).format(DateTimeFormatter.ISO_DATE))
            .build()
        }
        .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("""{ "charges": [] } """)
    }
  }
}
