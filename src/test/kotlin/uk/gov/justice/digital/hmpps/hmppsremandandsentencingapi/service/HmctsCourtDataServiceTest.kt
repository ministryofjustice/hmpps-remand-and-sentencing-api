package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class HmctsCourtDataServiceTest {

  @Mock
  lateinit var courtDataIngestionApi: CourtDataIngestionApiClient

  @Mock
  lateinit var documentManagementApi: DocumentManagementApiClient

  @Mock
  lateinit var appearanceOutcomeService: AppearanceOutcomeService

  @InjectMocks
  lateinit var service: HmctsCourtDataService

  @Test
  fun `should map hearing into court appearance with sentencing warrant`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDateTime.of(2025, 1, 1, 10, 0),
      caseReferences = listOf("CASE123"),
      documents = listOf(
        HmctsCourHearingDocument(
          documentId = documentId,
          documentType = "SENTENCING_WARRANT",
        ),
      ),
      courtName = "Court",
      hearingType = "Hearing",
    )

    val document = DocumentManagementApiDocument(
      documentUuid = documentId,
      documentFilename = "sentencing-warrant.pdf",
    )

    `when`(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    `when`(
      documentManagementApi.getDocumentsByIds(listOf(documentId.toString())),
    ).thenReturn(listOf(document))

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertEquals(hearingId, result.appearanceUuid)
    assertEquals("CASE123", result.courtCaseReference)
    assertEquals("SENTENCING", result.warrantType)
    assertEquals(EventSource.DPS, result.source)
    assertEquals(DeleteCourtAppearanceStatus.SUPPORTED, result.deleteStatus)

    assertEquals(1, result.documents.size)
    assertEquals("HMCTS_WARRANT", result.documents.first().documentType)
    assertEquals("sentencing-warrant.pdf", result.documents.first().fileName)

    verify(courtDataIngestionApi).getCourtHearing(hearingId)
  }

  @Test
  fun `should resolve remand warrant outcome`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()

    val outcomeUuid =
      UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8")

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDateTime.now(),
      caseReferences = emptyList(),
      documents = listOf(
        HmctsCourHearingDocument(
          documentId = documentId,
          documentType = "REMAND_WARRANT",
        ),
      ),
      courtName = "Court",
      hearingType = "Hearing",
    )

    val outcome = mock<CourtAppearanceOutcome>()

    `when`(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    `when`(documentManagementApi.getDocumentsByIds(listOf(documentId.toString())))
      .thenReturn(emptyList())

    `when`(appearanceOutcomeService.findByUuid(outcomeUuid))
      .thenReturn(outcome)

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertNotNull(result.outcome)
    assertEquals(outcome, result.outcome)
    assertEquals("NON_SENTENCING", result.warrantType)

    verify(appearanceOutcomeService).findByUuid(outcomeUuid)
  }

  @Test
  fun `should map prison court register document type`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDateTime.now(),
      caseReferences = emptyList(),
      documents = listOf(
        HmctsCourHearingDocument(
          documentId = documentId,
          documentType = "PRISON_COURT_REGISTER",
        ),
      ),
      courtName = "Court",
      hearingType = "Hearing",
    )

    `when`(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    `when`(documentManagementApi.getDocumentsByIds(listOf(documentId.toString())))
      .thenReturn(emptyList())

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertEquals(
      "PRISON_COURT_REGISTER",
      result.documents.first().documentType,
    )
  }

  @Test
  fun `should use unknown filename when document not found`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDateTime.now(),
      caseReferences = emptyList(),
      documents = listOf(
        HmctsCourHearingDocument(
          documentId = documentId,
          documentType = "SENTENCING_WARRANT",
        ),
      ),
      courtName = "Court",
      hearingType = "Hearing",
    )

    `when`(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    `when`(documentManagementApi.getDocumentsByIds(anyList()))
      .thenReturn(emptyList())

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertEquals(
      "Unknown filename",
      result.documents.first().fileName,
    )
  }

  private fun <T> anyList(): List<T> = ArgumentMatchers.anyList<T>()
}
