package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ConsecutiveChainValidationRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceDetailsForConsecValidation
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class ValidateConsecutiveChainTests : IntegrationTestBase() {
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
      SentenceDetailsForConsecValidation(
        sentenceUuid = it.sentenceUuid,
        consecutiveToSentenceUuid = it.consecutiveTo?.sentenceUuid,
      )
    }

    val sourceId =
      courtCase.second.appearances[0].charges.first { it.sentence?.chargeNumber == "1" }.sentence!!.sentenceUuid!!
    val targetId = courtCase2.second.appearances[0].charges[0].sentence!!.sentenceUuid!!

    val consecDetails = ConsecutiveChainValidationRequest(
      prisonerId = courtCase.second.prisonerId,
      appearanceUuid = courtCase.second.appearances[0].appearanceUuid,
      sourceSentenceUuid = sourceId,
      targetSentenceUuid = targetId,
      sentences = sentences,
    )

    val response = webTestClient
      .post()
      .uri("/sentence/consecutive-chain/has-a-loop")
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
