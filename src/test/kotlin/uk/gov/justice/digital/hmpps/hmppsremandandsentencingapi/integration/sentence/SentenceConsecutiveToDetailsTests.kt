package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.ConsecutiveChainCheckRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ConsecutiveSentenceDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceConsecutiveToDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class SentenceConsecutiveToDetailsTests : IntegrationTestBase() {

  @Test
  fun `returns sentences to chain to grouped by appearance when there is an active sentence in the past`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val result = webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentenceConsecutiveToDetailsResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.sentences).hasSize(1)
    val sentenceConsecutiveToDetails = result.sentences[0]
    Assertions.assertThat(sentenceConsecutiveToDetails.appearanceDate).isEqualTo(appearance.appearanceDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.courtCode).isEqualTo(appearance.courtCode)
    Assertions.assertThat(sentenceConsecutiveToDetails.courtCaseReference).isEqualTo(appearance.courtCaseReference)
    Assertions.assertThat(sentenceConsecutiveToDetails.sentenceUuid).isEqualTo(sentence.sentenceUuid!!)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceCode).isEqualTo(charge.offenceCode)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceStartDate).isEqualTo(charge.offenceStartDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceEndDate).isEqualTo(charge.offenceEndDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.countNumber).isEqualTo(sentence.chargeNumber)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val sentence = appearance.charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val sentence = appearance.charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Nested
  inner class ValidateConsecutiveChainTests {
    @Test
    fun `should recognise a looped chain of consecutive sentences when the target sentence is already in a chain`() {
      // This tests when the 'target' is held on another court case in the consec chain
      val s1 = DpsDataCreator.dpsCreateSentence(
        chargeNumber = "1",
        sentenceServeType = "FORTHWITH",
        sentenceReference = "0",
      )

      val s2 = DpsDataCreator.dpsCreateSentence(
        chargeNumber = "2",
        sentenceServeType = "CONSECUTIVE",
        sentenceReference = "1",
        consecutiveToSentenceReference = "0",
      )

      val s3 = DpsDataCreator.dpsCreateSentence(
        chargeNumber = "3",
        sentenceServeType = "CONSECUTIVE",
        sentenceReference = "2",
        consecutiveToSentenceReference = "1",
      )

      val s4 = DpsDataCreator.dpsCreateSentence(
        chargeNumber = "4",
        sentenceServeType = "CONSECUTIVE",
        sentenceReference = "3",
        consecutiveToSentenceReference = "2",
      )

      val c1 = DpsDataCreator.dpsCreateCharge(sentence = s1)
      val c2 = DpsDataCreator.dpsCreateCharge(sentence = s2)
      val c3 = DpsDataCreator.dpsCreateCharge(sentence = s3)
      val c4 = DpsDataCreator.dpsCreateCharge(sentence = s4)

      val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(c4, c3, c2, c1))

      val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

      val sentencesUUIDs = courtCase.second.appearances[0].charges.map { it.sentence!!.sentenceUuid!! }

      val sentenceEntities = sentenceRepository.findBySentenceUuidIn(sentencesUUIDs)

      val sentenceUUIDCount3 = sentenceEntities.first { it.countNumber == "3" }.sentenceUuid

      // Case 2 (CC2): single sentence, consecutive to CC1 Count 3
      val sentenceInCC2 = DpsDataCreator.dpsCreateSentence(
        chargeNumber = "5",
        sentenceReference = "1",
        consecutiveToSentenceUuid = sentenceUUIDCount3,
      )
      val chargeInCC2 = DpsDataCreator.dpsCreateCharge(sentence = sentenceInCC2)
      val appearance2 = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(chargeInCC2))
      val courtCase2 = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance2)))

      val sentences = sentenceEntities.map {
        ConsecutiveSentenceDetails(
          sentenceUuid = it.sentenceUuid,
          consecutiveToSentenceUuid = it.consecutiveTo?.sentenceUuid,
        )
      }

      val sourceId = courtCase.second.appearances[0].charges.first { it.sentence?.chargeNumber == "1" }.sentence!!.sentenceUuid!!
      val targetId = courtCase2.second.appearances[0].charges[0].sentence!!.sentenceUuid!!

      val consecDetails = ConsecutiveChainCheckRequest(
        prisonerId = courtCase.second.prisonerId,
        appearanceUuid = courtCase.second.appearances[0].appearanceUuid,
        sourceSentenceUuid = sourceId,
        targetSentenceUuid = targetId,
        sentences = sentences,
      )

      val response = webTestClient
        .post()
        .uri("/sentences/consecutive-chain/check")
        .bodyValue(consecDetails)
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
          it.contentType = MediaType.APPLICATION_JSON
        }
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Boolean::class.java)
        .responseBody.blockFirst()!!

      assertTrue(response)
    }
  }
}
