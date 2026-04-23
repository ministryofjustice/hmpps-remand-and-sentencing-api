package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.description
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateSentenceTypeTests : IntegrationTestBase() {

  @Test
  fun `create sentence type`() {
    val createSentenceType = DpsDataCreator.createSentenceType()
    webTestClient.post()
      .uri("/sentence-type")
      .bodyValue(createSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.description")
      .isEqualTo(createSentenceType.description)
      .jsonPath("$.nomisCjaCode")
      .isEqualTo(createSentenceType.nomisCjaCode)
  }

  @Test
  fun `adding a sentence type with a blank description results in error`() {
    val createSentenceType = DpsDataCreator.createSentenceType(description = "")
    webTestClient.post()
      .uri("/sentence-type")
      .bodyValue(createSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("description")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("You must enter a description")
  }

  @Test
  fun `adding a sentence type with a null classification results in error`() {
    val createSentenceType = DpsDataCreator.createSentenceType(classification = null)
    webTestClient.post()
      .uri("/sentence-type")
      .bodyValue(createSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("classification")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("You must select a Classification")
  }

  @Test
  fun `trying to create a sentence type with a NOMIS code that is already mapped results in error`() {
    val createSentenceType = DpsDataCreator.createSentenceType()
    webTestClient.post()
      .uri("/sentence-type")
      .bodyValue(createSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
    webTestClient.post()
      .uri("/sentence-type")
      .bodyValue(createSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("nomisCjaCode")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("CJA code and Sentence Calc Type combination is already mapped")
  }

  @Test
  fun `migrate legacy sentence records to the new supported sentence type`() {
    val (sentenceUuid, createdSentence) = createLegacySentence()
    val sentenceTypeUuid = UUID.randomUUID()
    val createSentenceType = DpsDataCreator.createSentenceType(
      sentenceTypeUuid = sentenceTypeUuid,
      nomisCjaCode = createdSentence.legacyData.sentenceCategory!!,
      nomisSentenceCalcType = createdSentence.legacyData.sentenceCalcType!!,
      description = "A new supported sentence type",
    )
    webTestClient.post()
      .uri("/sentence-type")
      .bodyValue(createSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated

    await untilCallTo {
      sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid)?.sentenceType
    } matches { it != null }

    webTestClient
      .get()
      .uri("/court-appearance/${createdSentence.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.chargeUuid == '${createdSentence.chargeUuids.first()}')].sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo(sentenceTypeUuid.toString())
  }
}
