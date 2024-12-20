package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FOURTEEN_DAY_FIXED_TERM_RECALL
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.HDC_STANDARD_RECALL
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RecallIntTests : IntegrationTestBase() {

  @Test
  fun `Create recall and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      recallDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallType = FOURTEEN_DAY_FIXED_TERM_RECALL,
      createdByUsername = "user001",
    )

    val createRecall = postRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUniqueIdentifier = createRecall.recallUuid,
          prisonerId = "A12345B",
          recallDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FOURTEEN_DAY_FIXED_TERM_RECALL,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
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
            recallUniqueIdentifier = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            prisonerId = "A12345B",
            recallDate = LocalDate.of(2024, 7, 1),
            returnToCustodyDate = LocalDate.of(2024, 7, 1),
            recallType = HDC_STANDARD_RECALL,
            createdByUsername = "admin_user",
            createdAt = ZonedDateTime.now(),
          ),
          Recall(
            recallUniqueIdentifier = UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
            prisonerId = "A12345B",
            recallDate = LocalDate.of(2024, 7, 2),
            returnToCustodyDate = LocalDate.of(2024, 7, 2),
            recallType = HDC_STANDARD_RECALL,
            createdByUsername = "admin_user",
            createdAt = ZonedDateTime.now(),
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
        recallType = FOURTEEN_DAY_FIXED_TERM_RECALL,
        recallDate = originalRecall.recallDate,
        returnToCustodyDate = originalRecall.returnToCustodyDate,
        createdByUsername = "user001",
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUniqueIdentifier = uuid,
          prisonerId = originalRecall.prisonerId,
          recallDate = originalRecall.recallDate,
          returnToCustodyDate = originalRecall.returnToCustodyDate,
          recallType = FOURTEEN_DAY_FIXED_TERM_RECALL,
          createdByUsername = originalRecall.createdByUsername,
          createdAt = originalRecall.createdAt,
        ),
      )
  }

  private fun getRecallByUUID(recallUuid: UUID): Recall =
    webTestClient
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

  private fun getRecallsByPrisonerId(prisonerId: String): List<Recall> =
    webTestClient
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

  private fun postRecall(recall: CreateRecall) =
    webTestClient
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

  private fun putRecall(recall: CreateRecall, uuid: UUID) =
    webTestClient
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
