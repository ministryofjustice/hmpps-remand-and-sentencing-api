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

  private val unknownPreRecallSentenceTypeUuid = uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService.recallSentenceTypeBucketUuid

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
    val sentenceUuid = createdCourtCase.appearances.first().charges.first().sentence?.sentenceUuid
      ?: throw IllegalStateException("Expected sentence to exist")

    // Get valid sentence type UUIDs
    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceTypeId = sdsType,
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
      .jsonPath("$.updatedSentenceUuids").isArray
      .jsonPath("$.updatedSentenceUuids.length()").isEqualTo(1)
      .jsonPath("$.updatedSentenceUuids[0]").isEqualTo(sentenceUuid.toString())

    // Verify the sentence was updated
    val updatedSentence = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid)
      ?: throw IllegalStateException("Expected updated sentence to exist")
    assertThat(updatedSentence.sentenceType?.sentenceTypeUuid).isEqualTo(sdsType)

    // Verify domain event was emitted
    val messages = getMessages(1)
    assertThat(messages).hasSize(1)
    val message = messages[0]
    assertThat(message.eventType).isEqualTo("sentence.updated")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("DPS")
    assertThat(message.additionalInformation.get("sentenceId").asText()).isEqualTo(sentenceUuid.toString())
  }

  @Test
  fun `return 404 when court case not found`() {
    val nonExistentCourtCaseUuid = UUID.randomUUID()
    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = UUID.randomUUID(),
          sentenceTypeId = UUID.randomUUID(),
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
      .jsonPath("$.userMessage").isEqualTo("not found: Court case with UUID $nonExistentCourtCaseUuid not found")
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
    val sentenceUuid = createdCourtCase.appearances.first().charges.first().sentence?.sentenceUuid
      ?: throw IllegalStateException("Expected sentence to exist")

    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceTypeId = sdsType,
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
      .sentenceTypeUuid

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = nonExistentSentenceUuid,
          sentenceTypeId = sdsType,
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
      .jsonPath("$.userMessage").isEqualTo("not found: Sentence with UUID $nonExistentSentenceUuid not found in court case")
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
    val sentenceUuid = createdCourtCase.appearances.first().charges.first().sentence?.sentenceUuid
      ?: throw IllegalStateException("Expected sentence to exist")
    val invalidSentenceType = UUID.randomUUID()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceTypeId = invalidSentenceType,
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
      .jsonPath("$.userMessage").isEqualTo("not found: Sentence type '$invalidSentenceType' is not a valid sentence type")
      .jsonPath("$.developerMessage").isEqualTo("Sentence type '$invalidSentenceType' is not a valid sentence type")
  }

  @Test
  fun `return 401 when no auth token provided`() {
    val courtCaseUuid = UUID.randomUUID()
    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = UUID.randomUUID(),
          sentenceTypeId = UUID.randomUUID(),
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
          sentenceTypeId = UUID.randomUUID(),
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
    val firstSentenceUuid = charges[0].sentence?.sentenceUuid
      ?: throw IllegalStateException("Expected first sentence to exist")
    val secondSentenceUuid = charges[1].sentence?.sentenceUuid
      ?: throw IllegalStateException("Expected second sentence to exist")

    // Get valid sentence type UUIDs
    val sdsType = sentenceTypeRepository.findAll()
      .first { it.description == "SDS (Standard Determinate Sentence)" }
      .sentenceTypeUuid

    val edsType = sentenceTypeRepository.findAll()
      .first { it.description == "EDS (Extended Determinate Sentence)" }
      .sentenceTypeUuid

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = firstSentenceUuid,
          sentenceTypeId = sdsType,
        ),
        SentenceTypeUpdate(
          sentenceUuid = secondSentenceUuid,
          sentenceTypeId = edsType,
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
      .jsonPath("$.updatedSentenceUuids").isArray
      .jsonPath("$.updatedSentenceUuids.length()").isEqualTo(2)
      .jsonPath("$.updatedSentenceUuids[0]").isEqualTo(firstSentenceUuid.toString())
      .jsonPath("$.updatedSentenceUuids[1]").isEqualTo(secondSentenceUuid.toString())

    // Verify both sentences were updated
    val updatedFirstSentence = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(firstSentenceUuid)
      ?: throw IllegalStateException("Expected first updated sentence to exist")
    assertThat(updatedFirstSentence.sentenceType?.sentenceTypeUuid).isEqualTo(sdsType)

    val updatedSecondSentence = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(secondSentenceUuid)
      ?: throw IllegalStateException("Expected second updated sentence to exist")
    assertThat(updatedSecondSentence.sentenceType?.sentenceTypeUuid).isEqualTo(edsType)

    // Verify domain events were emitted
    val messages = getMessages(2)
    assertThat(messages).hasSize(2)
    messages.forEach { message ->
      assertThat(message.eventType).isEqualTo("sentence.updated")
      assertThat(message.additionalInformation.get("source").asText()).isEqualTo("DPS")
    }
    val sentenceIdsInMessages = messages.map { it.additionalInformation.get("sentenceId").asText() }.toSet()
    assertThat(sentenceIdsInMessages).containsExactlyInAnyOrder(firstSentenceUuid.toString(), secondSentenceUuid.toString())
  }
}
