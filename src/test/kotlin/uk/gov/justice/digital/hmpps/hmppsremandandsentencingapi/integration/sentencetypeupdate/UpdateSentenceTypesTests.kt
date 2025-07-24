package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetypeupdate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class UpdateSentenceTypesTests : IntegrationTestBase() {

  @Autowired
  private lateinit var sentenceTypeRepository: SentenceTypeRepository

  private val unknownPreRecallSentenceTypeUuid = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")

  @Test
  fun `successfully update sentence types for unknown pre-recall sentences`() {
    // Create court case with sentence
    val courtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(
        DpsDataCreator.dpsCreateCourtAppearance(
          charges = listOf(
            DpsDataCreator.dpsCreateCharge(
              sentence = DpsDataCreator.dpsCreateSentence(sentenceTypeId = unknownPreRecallSentenceTypeUuid),
            ),
          ),
        ),
      ),
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(courtCase)
    val sentenceUuid = createdCourtCase.appearances.first().charges.first().sentence!!.sentenceUuid!!

    // Get valid sentence type UUIDs
    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid.toString()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsType,
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.updatedCount").isEqualTo(1)
      .jsonPath("$.updates[0].sentenceUuid").isEqualTo(sentenceUuid.toString())
      .jsonPath("$.updates[0].sentenceType").isEqualTo(sdsType)

    // Verify the sentence was updated
    val updatedSentence = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid)!!
    assertThat(updatedSentence.sentenceType?.sentenceTypeUuid.toString()).isEqualTo(sdsType)
  }

  @Test
  fun `return 404 when court case not found`() {
    val nonExistentCourtCaseUuid = UUID.randomUUID()
    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = UUID.randomUUID(),
          sentenceType = UUID.randomUUID().toString(),
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$nonExistentCourtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Court case or sentence not found")
      .jsonPath("$.developerMessage").isEqualTo("Court case with UUID $nonExistentCourtCaseUuid not found")
  }

  @Test
  fun `return 400 when request has empty updates array`() {
    val courtCase = DpsDataCreator.dpsCreateCourtCase()
    val (courtCaseUuid, _) = createCourtCase(courtCase)

    val request = UpdateSentenceTypeRequest(updates = emptyList())

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> { message ->
        assertThat(message).contains("Updates list cannot be empty")
      }
  }

  @Test
  fun `return 422 when sentence does not have unknown pre-recall sentence type`() {
    // Create court case with regular sentence
    val courtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(
        DpsDataCreator.dpsCreateCourtAppearance(
          charges = listOf(
            DpsDataCreator.dpsCreateCharge(
              sentence = DpsDataCreator.dpsCreateSentence(), // Default sentence type
            ),
          ),
        ),
      ),
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(courtCase)
    val sentenceUuid = createdCourtCase.appearances.first().charges.first().sentence!!.sentenceUuid!!

    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid.toString()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsType,
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(422)
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Cannot update sentence type")
      .jsonPath("$.developerMessage").isEqualTo("Sentence $sentenceUuid does not have type 'unknown pre-recall sentence'")
  }

  @Test
  fun `return 404 when sentence not found in court case`() {
    val courtCase = DpsDataCreator.dpsCreateCourtCase()
    val (courtCaseUuid, _) = createCourtCase(courtCase)
    val nonExistentSentenceUuid = UUID.randomUUID()

    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid.toString()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = nonExistentSentenceUuid,
          sentenceType = sdsType,
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Court case or sentence not found")
      .jsonPath("$.developerMessage").isEqualTo("Sentence with UUID $nonExistentSentenceUuid not found in court case")
  }

  @Test
  fun `return 404 when invalid sentence type provided`() {
    val courtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(
        DpsDataCreator.dpsCreateCourtAppearance(
          charges = listOf(
            DpsDataCreator.dpsCreateCharge(
              sentence = DpsDataCreator.dpsCreateSentence(sentenceTypeId = unknownPreRecallSentenceTypeUuid),
            ),
          ),
        ),
      ),
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(courtCase)
    val sentenceUuid = createdCourtCase.appearances.first().charges.first().sentence!!.sentenceUuid!!
    val invalidSentenceType = UUID.randomUUID().toString()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = invalidSentenceType,
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Court case or sentence not found")
      .jsonPath("$.developerMessage").isEqualTo("Sentence type '$invalidSentenceType' is not a valid sentence type")
  }

  @Test
  fun `return 401 when no auth token provided`() {
    val courtCaseUuid = UUID.randomUUID()
    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = UUID.randomUUID(),
          sentenceType = UUID.randomUUID().toString(),
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `return 403 when user does not have required role`() {
    val courtCaseUuid = UUID.randomUUID()
    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = UUID.randomUUID(),
          sentenceType = UUID.randomUUID().toString(),
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_SOME_OTHER_ROLE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `successfully update multiple sentences atomically`() {
    // Create court case with multiple sentences having unknown pre-recall sentence type
    val courtCase = DpsDataCreator.dpsCreateCourtCase(
      appearances = listOf(
        DpsDataCreator.dpsCreateCourtAppearance(
          charges = listOf(
            DpsDataCreator.dpsCreateCharge(
              sentence = DpsDataCreator.dpsCreateSentence(sentenceTypeId = unknownPreRecallSentenceTypeUuid),
            ),
            DpsDataCreator.dpsCreateCharge(
              sentence = DpsDataCreator.dpsCreateSentence(sentenceTypeId = unknownPreRecallSentenceTypeUuid),
            ),
          ),
        ),
      ),
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(courtCase)
    val charges = createdCourtCase.appearances.first().charges
    val firstSentenceUuid = charges[0].sentence!!.sentenceUuid!!
    val secondSentenceUuid = charges[1].sentence!!.sentenceUuid!!

    // Get valid sentence type UUIDs
    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid.toString()

    val edsType = sentenceTypeRepository.findAll()
      .first { it.description == "EDS (Extended Determinate Sentence)" }
      .sentenceTypeUuid.toString()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = firstSentenceUuid,
          sentenceType = sdsType,
        ),
        SentenceTypeUpdate(
          sentenceUuid = secondSentenceUuid,
          sentenceType = edsType,
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/court-case/$courtCaseUuid/sentences/update-types")
      .bodyValue(request)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.updatedCount").isEqualTo(2)
      .jsonPath("$.updates[0].sentenceUuid").isEqualTo(firstSentenceUuid.toString())
      .jsonPath("$.updates[0].sentenceType").isEqualTo(sdsType)
      // Success field removed - operation is atomic
      .jsonPath("$.updates[1].sentenceUuid").isEqualTo(secondSentenceUuid.toString())
      .jsonPath("$.updates[1].sentenceType").isEqualTo(edsType)
      .jsonPath("$.updates[1].success").isEqualTo(true)

    // Verify both sentences were updated
    val updatedFirstSentence = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(firstSentenceUuid)!!
    assertThat(updatedFirstSentence.sentenceType?.sentenceTypeUuid.toString()).isEqualTo(sdsType)

    val updatedSecondSentence = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(secondSentenceUuid)!!
    assertThat(updatedSecondSentence.sentenceType?.sentenceTypeUuid.toString()).isEqualTo(edsType)
  }
}
