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

class UpdateSentenceTypeTests : IntegrationTestBase() {

  @Test
  fun `update sentence type`() {
    val updateSentenceType = DpsDataCreator.createSentenceType()
    webTestClient.put()
      .uri("/sentence-type/${UUID.randomUUID()}")
      .bodyValue(updateSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.description")
      .isEqualTo(updateSentenceType.description)
      .jsonPath("$.nomisCjaCode")
      .isEqualTo(updateSentenceType.nomisCjaCode)
  }

  @Test
  fun `updating a sentence type with a blank description results in error`() {
    val updateSentenceType = DpsDataCreator.createSentenceType(description = "")
    webTestClient.put()
      .uri("/sentence-type/${UUID.randomUUID()}")
      .bodyValue(updateSentenceType)
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
  fun `trying to update a sentence type with a NOMIS code that is already mapped results in error`() {
    val updateSentenceType = DpsDataCreator.createSentenceType()
    webTestClient.put()
      .uri("/sentence-type/${UUID.randomUUID()}")
      .bodyValue(updateSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
    webTestClient.put()
      .uri("/sentence-type/${UUID.randomUUID()}")
      .bodyValue(updateSentenceType)
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
  fun `migrate legacy sentence records to the new updated supported sentence type`() {
    val (sentenceUuid, createdSentence) = createLegacySentence()
    val sentenceTypeUuid = UUID.randomUUID()
    val updateSentenceType = DpsDataCreator.createSentenceType(
      sentenceTypeUuid = sentenceTypeUuid,
      nomisCjaCode = createdSentence.legacyData.sentenceCategory!!,
      nomisSentenceCalcType = createdSentence.legacyData.sentenceCalcType!!,
      description = "A new supported sentence type",
    )
    webTestClient.put()
      .uri("/sentence-type/${UUID.randomUUID()}")
      .bodyValue(updateSentenceType)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

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
