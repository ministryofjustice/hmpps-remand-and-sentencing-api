package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class GetPersonDocumentsTests : IntegrationTestBase() {

  @Test
  fun `provide no query parameters returns all documents`() {
    val (document) = uploadDocument()
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(documents = listOf(document))
    val(_, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    webTestClient
      .get()
      .uri("/person/${courtCase.prisonerId}/documents")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseDocuments[0].appearanceDocumentsByType.${document.documentType}.[0].documentUUID")
      .isEqualTo(document.documentUUID)
  }

  @Test
  fun `match on case reference returns the document`() {
    val (document) = uploadDocument()
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(documents = listOf(document))
    val(courtCaseUuid, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val (otherDocument) = uploadDocument()
    val otherAppearance = DpsDataCreator.dpsCreateCourtAppearance(documents = listOf(otherDocument), courtCaseReference = "OTHERREF")
    val(otherCourtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(otherAppearance)))
    webTestClient
      .get()
      .uri {
        it.path("/person/${courtCase.prisonerId}/documents")
          .queryParam("keyword", appearance.courtCaseReference)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseDocuments[?(@.courtCaseUuid == '$courtCaseUuid')].appearanceDocumentsByType.${document.documentType}[0].documentUUID")
      .isEqualTo(document.documentUUID.toString())
      .jsonPath("$.courtCaseDocuments[?(@.courtCaseUuid == '$otherCourtCaseUuid')]")
      .doesNotExist()
  }

  @Test
  fun `match on warrant type document type combination`() {
    val document = DpsDataCreator.dpsCreateUploadedDocument()
    val otherDocument = DpsDataCreator.dpsCreateUploadedDocument(documentType = "OTHER_DOC_TYPE")
    uploadDocument(listOf(document, otherDocument))
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(documents = listOf(document, otherDocument))
    val(courtCaseUuid, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    webTestClient
      .get()
      .uri {
        it.path("/person/${courtCase.prisonerId}/documents")
          .queryParam("warrantTypeDocumentTypes", "${appearance.warrantType}|${document.documentType}")
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseDocuments[?(@.courtCaseUuid == '$courtCaseUuid')].appearanceDocumentsByType.${document.documentType}[0].documentUUID")
      .isEqualTo(document.documentUUID.toString())
      .jsonPath("$.courtCaseDocuments[?(@.courtCaseUuid == '$courtCaseUuid')].appearanceDocumentsByType.${otherDocument.documentType}")
      .doesNotExist()
  }

  @Test
  fun `filter by court codes returns only matching documents`() {
    // two docs in two different courts
    val shfDoc = DpsDataCreator.dpsCreateUploadedDocument()
    val manDoc = DpsDataCreator.dpsCreateUploadedDocument()
    uploadDocument(listOf(shfDoc, manDoc))

    val shfAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      documents = listOf(shfDoc),
      courtCode = "SHF",
      courtCaseReference = "SHF-REF",
    )
    val manAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      documents = listOf(manDoc),
      courtCode = "MAN",
      courtCaseReference = "MAN-REF",
    )

    val (shfCourtCaseUuid, shfCourtCase) =
      createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(shfAppearance)))
    val (manCourtCaseUuid) =
      createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(manAppearance)))

    webTestClient
      .get()
      .uri {
        it.path("/person/${shfCourtCase.prisonerId}/documents")
          // pass the list param (single value here). You can pass multiple values:
          // .queryParam("courtCodes", "SHF", "MAN")
          .queryParam("courtCodes", "SHF")
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      // SHF document is present
      .jsonPath("$.courtCaseDocuments[?(@.courtCaseUuid == '$shfCourtCaseUuid')].appearanceDocumentsByType.${shfDoc.documentType}[0].documentUUID")
      .isEqualTo(shfDoc.documentUUID.toString())
      // MAN court case is filtered out entirely
      .jsonPath("$.courtCaseDocuments[?(@.courtCaseUuid == '$manCourtCaseUuid')]")
      .doesNotExist()
  }
}
