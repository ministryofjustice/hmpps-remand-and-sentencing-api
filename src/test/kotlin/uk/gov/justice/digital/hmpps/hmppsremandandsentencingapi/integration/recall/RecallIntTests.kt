package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
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
        ),
      )
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
        ),
      )
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
        createdByPrison = originalRecall.createdByPrison,
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
        ),
      )
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

  private fun getRecallsByPrisonerId(prisonerId: String): List<Recall> = webTestClient
    .get()
    .uri("/recall/person/$prisonerId")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBodyList(Recall::class.java)
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
