package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import java.util.UUID

@Component
class HmctsCourtDataService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val documentService: DocumentManagementApiClient,
  val courtRegisterApiClient: CourtRegisterApiClient,
) {

  fun getCourtAppearanceFromHmctsHearingId(courtHearingId: UUID): CourtAppearance {
    val hearing = courtDataIngestionApi.getCourtHearing(courtHearingId)
    val documents = documentService.getDocumentsByIds(hearing.documents.map { it.documentId.toString() })
      .filter { it.duplicateOf == null }
    val court = courtRegisterApiClient.getCourtRegisterByHmctsId(hearing.courtId)

    return CourtAppearance(
      appearanceUuid = courtHearingId,
      outcome = null,
      courtCode = court?.courtId ?: hearing.courtId.toString(),
      courtCaseReference = hearing.caseReferences.firstOrNull(),
      criminalAppealOfficeReference = null,
      appearanceDate = hearing.hearingDate.toLocalDate(),
      warrantType = mapWarrantType(hearing),
      nextCourtAppearance = null,
      charges = emptyList(),
      overallSentenceLength = null,
      overallConvictionDate = null,
      legacyData = null,
      documents = hearing.documents.mapNotNull {
        documents.find { document -> document.documentUuid == it.documentId }
          ?.let { document ->
            UploadedDocument(
              it.documentId,
              mapDocumentType(it.documentType),
              document.documentFilename,
            )
          }
      },
      source = EventSource.DPS,
      deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
    )
  }

  private fun mapDocumentType(documentType: String): String = if (documentType == "PRISON_COURT_REGISTER") {
    "PRISON_COURT_REGISTER"
  } else {
    "HMCTS_WARRANT"
  }

  private fun mapWarrantType(hearing: HmctsCourHearing): String = if (hearing.documents.any { it.documentType == "SENTENCING_WARRANT" }) {
    "SENTENCING"
  } else {
    "NON_SENTENCING"
  }
}
