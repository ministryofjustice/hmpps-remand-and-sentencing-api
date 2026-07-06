package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
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
  lateinit var courtRegisterApiClient: CourtRegisterApiClient

  @InjectMocks
  lateinit var service: HmctsCourtDataService

  @Test
  fun `should map hearing into court appearance with sentencing warrant`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()
    val courtId = UUID.randomUUID()

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = courtId,
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

    val courtRegister = CourtRegister(
      courtName = "Court",
      courtId = UUID.randomUUID().toString(),
      courtDescription = "Court description",
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

    `when`(courtRegisterApiClient.getCourtRegisterByHmctsId(courtId))
      .thenReturn(courtRegister)

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertEquals(hearingId, result.appearanceUuid)
    assertEquals("CASE123", result.courtCaseReference)
    assertEquals("SENTENCING", result.warrantType)
    assertEquals(EventSource.DPS, result.source)
    assertEquals(DeleteCourtAppearanceStatus.SUPPORTED, result.deleteStatus)

    assertEquals(1, result.documents.size)
    assertEquals("HMCTS_WARRANT", result.documents.first().documentType)
    assertEquals("sentencing-warrant.pdf", result.documents.first().fileName)
    assertEquals(courtRegister.courtId, result.courtCode)

    verify(courtDataIngestionApi).getCourtHearing(hearingId)
  }

  @Test
  fun `should map prison court register document type`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()
    val courtId = UUID.randomUUID()

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = courtId,
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
    val courtRegister = CourtRegister(
      courtName = "Court",
      courtId = UUID.randomUUID().toString(),
      courtDescription = "Court description",
    )

    `when`(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    `when`(documentManagementApi.getDocumentsByIds(listOf(documentId.toString())))
      .thenReturn(emptyList())

    `when`(courtRegisterApiClient.getCourtRegisterByHmctsId(courtId))
      .thenReturn(courtRegister)

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertEquals(
      "PRISON_COURT_REGISTER",
      result.documents.first().documentType,
    )
  }

  @Test
  fun `should use unknown filename when document not found and court not found`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()
    val courtId = UUID.randomUUID()

    val hearing = HmctsCourHearing(
      hearingId = hearingId,
      courtId = courtId,
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

    `when`(courtRegisterApiClient.getCourtRegisterByHmctsId(courtId))
      .thenReturn(null)

    `when`(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    `when`(documentManagementApi.getDocumentsByIds(anyList()))
      .thenReturn(emptyList())

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertEquals(
      "Unknown filename",
      result.documents.first().fileName,
    )
    assertEquals(
      courtId.toString(),
      result.courtCode,
    )
  }

  private fun <T> anyList(): List<T> = ArgumentMatchers.anyList<T>()
}
