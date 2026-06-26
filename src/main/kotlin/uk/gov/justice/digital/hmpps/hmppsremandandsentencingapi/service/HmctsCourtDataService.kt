package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
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
  val appearanceOutcomeService: AppearanceOutcomeService,
) {

  fun getCourtAppearanceFromHmctsHearingId(courtHearingId: UUID): CourtAppearance {
    val hearing = courtDataIngestionApi.getCourtHearing(courtHearingId)
    val documents = documentService.getDocumentsByIds(hearing.documents.map { it.documentId.toString() })
    val outcome = getOutcomeUuidFromDocumentTypes(hearing)?.let { appearanceOutcomeService.findByUuid(it) }

    return CourtAppearance(
      appearanceUuid = courtHearingId,
      outcome = outcome,
      courtCode = hearing.courtId.toString(), // TODO CDIA-169
      courtCaseReference = hearing.caseReferences.firstOrNull(),
      criminalAppealOfficeReference = null,
      appearanceDate = hearing.hearingDate.toLocalDate(),
      warrantType = mapWarrantType(hearing),
      nextCourtAppearance = null,
      charges = emptyList(),
      overallSentenceLength = null,
      overallConvictionDate = null,
      legacyData = null,
      documents = hearing.documents.map {
        val document = documents.find { document -> document.documentUuid == it.documentId }
        UploadedDocument(
          it.documentId,
          mapDocumentType(it.documentType),
          document?.documentFilename ?: "Unknown filename",
        )
      },
      source = EventSource.DPS,
      deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
    )
  }

  private fun getOutcomeUuidFromDocumentTypes(hearing: HmctsCourHearing): UUID? {
    if (hearing.documents.any { it.documentType == "REMAND_WARRANT" }) {
      return UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8")
    }
    return null
  }

  private fun mapDocumentType(documentType: String): String = if (documentType === "PRISON_COURT_REGISTER") {
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
