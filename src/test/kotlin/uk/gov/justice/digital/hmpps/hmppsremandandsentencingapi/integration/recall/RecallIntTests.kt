package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_28
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.LR_HDC
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RecallIntTests : IntegrationTestBase() {

  @Test
  fun `Create recall and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val createRecall = postRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Create recall with no dates and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = null,
      returnToCustodyDate = null,
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val createRecall = postRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = null,
          returnToCustodyDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Create recall with a uuid via edit endpoint`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_28,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val uuid = UUID.randomUUID()

    val createdRecall = putRecall(recall, uuid)

    assertThat(uuid).isEqualTo(createdRecall.recallUuid)
    val actualRecall = getRecallByUUID(createdRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createdRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_28,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Sql("classpath:test_data/insert-recalls.sql")
  @Test
  fun `Get all recalls for a prisoner`() {
    val recalls = getRecallsByPrisonerId("A12345B")

    assertThat(recalls)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          Recall(
            recallUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            prisonerId = "A12345B",
            revocationDate = LocalDate.of(2024, 7, 1),
            returnToCustodyDate = LocalDate.of(2024, 7, 1),
            recallType = LR_HDC,
            createdByUsername = "admin_user",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "HMI",
            sentences = emptyList(),
          ),
          Recall(
            recallUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
            prisonerId = "A12345B",
            revocationDate = LocalDate.of(2024, 7, 2),
            returnToCustodyDate = LocalDate.of(2024, 7, 2),
            recallType = LR_HDC,
            createdByUsername = "admin_user",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "HMI",
            sentences = emptyList(),
          ),
        ),
      )
  }

  @Sql("classpath:test_data/insert-recalls.sql")
  @Test
  fun `Update a recall`() {
    val uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val originalRecall = getRecallByUUID(uuid)

    putRecall(
      CreateRecall(
        prisonerId = "A12345B",
        recallTypeCode = FTR_14,
        revocationDate = originalRecall.revocationDate,
        returnToCustodyDate = originalRecall.returnToCustodyDate,
        createdByUsername = "user001",
        createdByPrison = "New prison",
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuid,
          prisonerId = originalRecall.prisonerId,
          revocationDate = originalRecall.revocationDate,
          returnToCustodyDate = originalRecall.returnToCustodyDate,
          recallType = FTR_14,
          createdByUsername = originalRecall.createdByUsername,
          createdAt = originalRecall.createdAt,
          createdByPrison = originalRecall.createdByPrison,
          sentences = emptyList(),
        ),
      )

    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.updated")
  }

  @Test
  @Sql("classpath:test_data/insert-sentences-to-recall.sql")
  fun `Create recall with a sentence and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
      sentenceIds = listOf(
        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
      ),
    )

    val createRecall = postRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    val expectedSentence = Sentence(
      sentenceUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
      chargeNumber = "1",
      periodLengths = emptyList(),
      sentenceServeType = "FORTHWITH",
      consecutiveToSentenceUuid = null,
      sentenceType = null,
      convictionDate = null,
      fineAmount = null,
      legacyData = null,
    )
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = listOf(expectedSentence),
          courtCaseIds = listOf("5725bfeb-23db-439f-ab4b-2ea4e74cd2b5"),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  @Sql("classpath:test_data/insert-sentences-to-recall.sql")
  fun `Create recall with two sentences and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
      sentenceIds = listOf(
        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        UUID.fromString("550e8400-e29b-41d4-a716-446655449999"),
      ),

    )

    val createRecall = postRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    val expectedSentence = Sentence(
      sentenceUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
      chargeNumber = "1",
      periodLengths = emptyList(),
      sentenceServeType = "FORTHWITH",
      consecutiveToSentenceUuid = null,
      sentenceType = null,
      convictionDate = null,
      fineAmount = null,
      legacyData = null,
    )
    val secondExpectedSentence = Sentence(
      sentenceUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655449999"),
      chargeNumber = "2",
      periodLengths = emptyList(),
      sentenceServeType = "CONCURRENT",
      consecutiveToSentenceUuid = null,
      sentenceType = null,
      convictionDate = null,
      fineAmount = null,
      legacyData = null,
    )
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = listOf(expectedSentence, secondExpectedSentence),
          courtCaseIds = listOf(
            "5725bfeb-23db-439f-ab4b-2ea4e74cd2b5",
            "846799d8-ce70-4a29-a630-382c904349ae",
          ),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  @Sql("classpath:test_data/insert-sentences-to-recall.sql")
  fun `Fetch created recall by UUID ignoring unrelated recall_sentences`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
      sentenceIds = emptyList(),
    )

    val createRecall = postRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }
  private fun getRecallByUUID(recallUuid: UUID): Recall = webTestClient
    .get()
    .uri("/recall/$recallUuid")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(Recall::class.java)
    .returnResult().responseBody!!

  private fun postRecall(recall: CreateRecall) = webTestClient
    .post()
    .uri("/recall")
    .bodyValue(recall)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isCreated
    .expectBody(SaveRecallResponse::class.java)
    .returnResult().responseBody!!

  private fun putRecall(recall: CreateRecall, uuid: UUID) = webTestClient
    .put()
    .uri("/recall/$uuid")
    .bodyValue(recall)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(SaveRecallResponse::class.java)
    .returnResult().responseBody!!
}
