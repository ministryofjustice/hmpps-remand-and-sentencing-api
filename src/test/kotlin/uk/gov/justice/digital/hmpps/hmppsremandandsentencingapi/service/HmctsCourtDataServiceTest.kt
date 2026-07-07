package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
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
  fun `should map hearing into court appearance with sentencing warrant removing duplicate documents`() {
    val hearingId = UUID.randomUUID()
    val documentId = UUID.randomUUID()
    val duplicateDocumentId = UUID.randomUUID()
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
        HmctsCourHearingDocument(
          documentId = duplicateDocumentId,
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
    val duplicateDocument = DocumentManagementApiDocument(
      documentUuid = duplicateDocumentId,
      documentFilename = "sentencing-warrant.pdf",
      duplicateOf = documentId,
    )

    whenever(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    whenever(documentManagementApi.getDocumentsByIds(listOf(documentId.toString(), duplicateDocumentId.toString())))
      .thenReturn(listOf(document, duplicateDocument))

    whenever(courtRegisterApiClient.getCourtRegisterByHmctsId(courtId))
      .thenReturn(courtRegister)

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertThat(result.appearanceUuid).isEqualTo(hearingId)
    assertThat(result.courtCaseReference).isEqualTo("CASE123")
    assertThat(result.warrantType).isEqualTo("SENTENCING")
    assertThat(result.source).isEqualTo(EventSource.DPS)
    assertThat(result.deleteStatus).isEqualTo(DeleteCourtAppearanceStatus.SUPPORTED)

    assertThat(result.documents).hasSize(1)
    assertThat(result.documents.first().documentType).isEqualTo("HMCTS_WARRANT")
    assertThat(result.documents.first().fileName).isEqualTo("sentencing-warrant.pdf")
    assertThat(result.courtCode).isEqualTo(courtRegister.courtId)

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
    val document = DocumentManagementApiDocument(
      documentUuid = documentId,
      documentFilename = "sentencing-warrant.pdf",
    )

    whenever(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    whenever(documentManagementApi.getDocumentsByIds(listOf(documentId.toString())))
      .thenReturn(listOf(document))

    whenever(courtRegisterApiClient.getCourtRegisterByHmctsId(courtId))
      .thenReturn(courtRegister)

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertThat(result.documents.first().documentType)
      .isEqualTo("PRISON_COURT_REGISTER")
  }

  @Test
  fun `should handle court not found`() {
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
    val document = DocumentManagementApiDocument(
      documentUuid = documentId,
      documentFilename = "sentencing-warrant.pdf",
    )

    whenever(courtRegisterApiClient.getCourtRegisterByHmctsId(courtId))
      .thenReturn(null)

    whenever(courtDataIngestionApi.getCourtHearing(hearingId))
      .thenReturn(hearing)

    whenever(documentManagementApi.getDocumentsByIds(listOf(documentId.toString())))
      .thenReturn(listOf(document))

    val result = service.getCourtAppearanceFromHmctsHearingId(hearingId)

    assertThat(result.courtCode)
      .isEqualTo(courtId.toString())
  }
}
