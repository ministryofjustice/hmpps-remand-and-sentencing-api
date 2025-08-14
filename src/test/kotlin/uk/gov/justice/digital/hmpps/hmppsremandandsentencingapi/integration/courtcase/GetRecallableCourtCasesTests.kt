package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class GetRecallableCourtCasesTests : IntegrationTestBase() {

  @Test
  fun `get all recallable court cases with sentenced status`() {
    // Create a court case that should appear in recallable list
    val sentencedCharge = DpsDataCreator.dpsCreateCharge(
      sentence = DpsDataCreator.dpsCreateSentence(),
      legacyData = ChargeLegacyData(
        postedDate = "2024-01-01",
        nomisOutcomeCode = "1002",
        outcomeDescription = "Guilty",
        outcomeDispositionCode = "C",
        outcomeConvictionFlag = true,
      ),
    )
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(sentencedCharge),
      outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"), // Sentenced outcome
      warrantType = "SENTENCING",
    )
    val sentencedCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencedAppearance))
    val (sentencedCourtCaseUuid, createdSentencedCase) = createCourtCase(sentencedCourtCase)

    // Create a remanded court case (should NOT appear in recallable list - no sentence)
    val remandedCharge = DpsDataCreator.dpsCreateCharge(sentence = null, outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"))
    val remandedAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(remandedCharge),
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"), // Remanded outcome
      warrantType = "REMAND",
    )
    val remandedCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(remandedAppearance))
    createCourtCase(remandedCourtCase)

    webTestClient
      .get()
      .uri("/court-case/${createdSentencedCase.prisonerId}/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(1)
      .jsonPath("$.cases[0].courtCaseUuid").isEqualTo(sentencedCourtCaseUuid)
      .jsonPath("$.cases[0].isSentenced").isEqualTo(true)
      .jsonPath("$.cases[0].date").exists()
      .jsonPath("$.cases[0].firstDayInCustody").exists()
      .jsonPath("$.cases[0].sentences.length()").isEqualTo(1)
      .jsonPath("$.cases[0].sentences[0].sentenceUuid").exists()
      .jsonPath("$.cases[0].sentences[0].offenceCode").exists()
      .jsonPath("$.cases[0].sentences[0].sentenceType").exists()
      .jsonPath("$.cases[0].sentences[0].sentenceTypeUuid").exists()
      .jsonPath("$.cases[0].sentences[0].isRecallable").isEqualTo(true)
      .jsonPath("$.cases[0].sentences[0].chargeLegacyData").exists()
      .jsonPath("$.cases[0].sentences[0].countNumber").isEqualTo("1")
      .jsonPath("$.cases[0].sentences[0].sentenceServeType").isEqualTo("FORTHWITH")
      .jsonPath("$.cases[0].sentences[0].sentenceLegacyData").doesNotExist()
      .jsonPath("$.cases[0].sentences[0].sentenceDate").exists()
  }

  @Test
  fun `get recallable court cases with sorting by date descending`() {
    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val appearance1 = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(charge1),
      warrantType = "SENTENCING",
      appearanceDate = LocalDate.of(2024, 1, 15),
    )
    val courtCase1 = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance1))
    val (courtCaseUuid1, createdCase1) = createCourtCase(courtCase1)

    val charge2 = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val appearance2 = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(charge2),
      warrantType = "SENTENCING",
      appearanceDate = LocalDate.of(2024, 1, 10),
    )
    val courtCase2 = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(appearance2),
      prisonerId = createdCase1.prisonerId, // Same prisoner
    )
    val (courtCaseUuid2) = createCourtCase(courtCase2)

    webTestClient
      .get()
      .uri("/court-case/${createdCase1.prisonerId}/recallable-court-cases?sortBy=date&sortOrder=desc")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(2)
      .jsonPath("$.cases[0].courtCaseUuid").isEqualTo(courtCaseUuid1) // More recent date first
      .jsonPath("$.cases[1].courtCaseUuid").isEqualTo(courtCaseUuid2)
  }

  @Test
  fun `get recallable court cases with sorting by date ascending`() {
    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val appearance1 = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(charge1),
      warrantType = "SENTENCING",
      appearanceDate = LocalDate.of(2024, 1, 15),
    )
    val courtCase1 = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance1))
    val (courtCaseUuid1, createdCase1) = createCourtCase(courtCase1)

    val charge2 = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val appearance2 = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(charge2),
      warrantType = "SENTENCING",
      appearanceDate = LocalDate.of(2024, 1, 10),
    )
    val courtCase2 = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(appearance2),
      prisonerId = createdCase1.prisonerId, // Same prisoner
    )
    val (courtCaseUuid2) = createCourtCase(courtCase2)

    webTestClient
      .get()
      .uri("/court-case/${createdCase1.prisonerId}/recallable-court-cases?sortBy=date&sortOrder=asc")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(2)
      .jsonPath("$.cases[0].courtCaseUuid").isEqualTo(courtCaseUuid2) // Earlier date first
      .jsonPath("$.cases[1].courtCaseUuid").isEqualTo(courtCaseUuid1)
  }

  @Test
  fun `get recallable court cases with correct authorized role`() {
    val charge = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(charge),
      warrantType = "SENTENCING",
    )
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (_, createdCase) = createCourtCase(courtCase)

    webTestClient
      .get()
      .uri("/court-case/${createdCase.prisonerId}/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(1)
  }

  @Test
  fun `returns empty list when no recallable court cases exist for prisoner`() {
    val charge = DpsDataCreator.dpsCreateCharge(sentence = null) // No sentence
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(charge),
      warrantType = "REMAND", // Not sentenced
    )
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (_, createdCase) = createCourtCase(courtCase)

    webTestClient
      .get()
      .uri("/court-case/${createdCase.prisonerId}/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(0)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/court-case/ABC123/recallable-court-cases")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `includes sentences from older appearances in the same court case`() {
    // Create a court case with multiple appearances
    val olderCharge = DpsDataCreator.dpsCreateCharge(
      sentence = DpsDataCreator.dpsCreateSentence(),
      legacyData = ChargeLegacyData(
        postedDate = "2024-01-01",
        nomisOutcomeCode = "1002",
        outcomeDescription = "Guilty",
        outcomeDispositionCode = "C",
        outcomeConvictionFlag = true,
      ),
    )
    val olderAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(olderCharge),
      warrantType = "SENTENCING",
      appearanceDate = LocalDate.of(2024, 1, 10),
    )

    val newerChargeWithoutSentence = DpsDataCreator.dpsCreateCharge(sentence = null)
    val newerAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(newerChargeWithoutSentence),
      warrantType = "SENTENCING",
      appearanceDate = LocalDate.of(2024, 1, 15),
    )

    // Court case has both appearances - latest has no sentence, older has sentence
    val courtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(olderAppearance, newerAppearance),
    )
    val (courtCaseUuid, createdCase) = createCourtCase(courtCase)

    webTestClient
      .get()
      .uri("/court-case/${createdCase.prisonerId}/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(1)
      .jsonPath("$.cases[0].courtCaseUuid").isEqualTo(courtCaseUuid)
      .jsonPath("$.cases[0].isSentenced").isEqualTo(true)
      .jsonPath("$.cases[0].sentences.length()").isEqualTo(1) // Should find the sentence from older appearance
      .jsonPath("$.cases[0].sentences[0].sentenceUuid").exists()
      .jsonPath("$.cases[0].sentences[0].isRecallable").isEqualTo(true)
      .jsonPath("$.cases[0].sentences[0].chargeLegacyData").exists()
      .jsonPath("$.cases[0].sentences[0].countNumber").isEqualTo("1")
      .jsonPath("$.cases[0].sentences[0].sentenceServeType").isEqualTo("FORTHWITH")
      .jsonPath("$.cases[0].sentences[0].sentenceLegacyData").doesNotExist()
      .jsonPath("$.cases[0].sentences[0].sentenceDate").exists()
  }

  @Test
  fun `Inactive sentences are returned by get recallable sentences endpoint`() {
    val (lifetimeUuid) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(active = false),
    )
    webTestClient
      .get()
      .uri("/court-case/${DpsDataCreator.Factory.DEFAULT_PRISONER_ID}/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(1)
      .jsonPath("$.cases[0].sentences[0].sentenceUuid").isEqualTo(lifetimeUuid.toString())
  }

  @Test
  fun `returns non-recallable sentence types`() {
    // Create a court case with a non-recallable sentence type (A/FINE)
    val chargeWithNonRecallableSentence = DpsDataCreator.dpsCreateCharge(
      sentence = DpsDataCreator.dpsCreateSentence(
        sentenceTypeId = UUID.fromString("c71ceefe-932b-4a69-b87c-7c1294e37cf7"), // A/FINE sentence type
      ),
    )
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(chargeWithNonRecallableSentence),
      warrantType = "SENTENCING",
    )
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid, createdCase) = createCourtCase(courtCase)

    webTestClient
      .get()
      .uri("/court-case/${createdCase.prisonerId}/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases.length()").isEqualTo(1)
      .jsonPath("$.cases[0].courtCaseUuid").isEqualTo(courtCaseUuid)
      .jsonPath("$.cases[0].sentences.length()").isEqualTo(1)
      .jsonPath("$.cases[0].sentences[0].isRecallable").isEqualTo(false)
      .jsonPath("$.cases[0].sentences[0].classification").isEqualTo("FINE")
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient.get()
      .uri("/court-case/ABC123/recallable-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
