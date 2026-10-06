package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.thingstodo.ThingsToDoTest.Companion.PRISONER_ID
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateUploadedDocumentTest : IntegrationTestBase() {

  @Test
  fun `create uploaded document`() {
    val documentUuid = UUID.randomUUID()
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    createUploadedDocument(createUploadedDocument)

    val uploadedDocument = uploadedDocumentRepository.findByDocumentUuid(documentUuid)
    assertThat(uploadedDocument).isNotNull
    assertThat(uploadedDocument?.documentType).isEqualTo(createUploadedDocument.documents.first().documentType)
    assertThat(uploadedDocument?.appearance).isNull()
  }

  @Test
  fun `create uploaded document will update if unattached to appearance`() {
    val documentUuid = UUID.randomUUID()
    val case = createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = PRISONER_ID))
    val appearanceId = case.second.appearances.first().appearanceUuid
    val unlinkedUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = appearanceId,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    createUploadedDocument(unlinkedUploadedDocument)
    createUploadedDocument(createUploadedDocument)

    val uploadedDocument = uploadedDocumentRepository.findByDocumentUuid(documentUuid)
    assertThat(uploadedDocument).isNotNull
    assertThat(uploadedDocument?.documentType).isEqualTo(createUploadedDocument.documents.first().documentType)
    assertThat(uploadedDocument?.appearance).isNotNull
  }

  @Test
  fun `create uploaded document will fail if existing document attached to appearance`() {
    val documentUuid = UUID.randomUUID()
    val case = createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = PRISONER_ID))
    val appearanceId = case.second.appearances.first().appearanceUuid
    val originalUploadedDocument = CreateUploadedDocument(
      appearanceUUID = appearanceId,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = appearanceId,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    createUploadedDocument(originalUploadedDocument)

    webTestClient
      .post()
      .uri("/uploaded-documents")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(createUploadedDocument)
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `no token results is unauthorized`() {
    val documentUuid = UUID.randomUUID()
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/uploaded-documents")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(createUploadedDocument)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val documentUuid = UUID.randomUUID()
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/uploaded-documents")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_UPLOADED_DOCUMENT_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(createUploadedDocument)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  private fun createUploadedDocument(createUploadedDocument: CreateUploadedDocument) = webTestClient
    .post()
    .uri("/uploaded-documents")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .bodyValue(createUploadedDocument)
    .exchange()
    .expectStatus()
    .isCreated
}
