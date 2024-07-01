package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FOURTEEN_DAY_FIXED_TERM_RECALL
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

  private fun postRecall(recall: CreateRecall) =
    webTestClient
      .post()
      .uri("/recall")
      .bodyValue(recall)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody(CreateRecallResponse::class.java)
      .returnResult().responseBody!!
}
