package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import java.util.UUID

// TODO add unit tests.
@Component
class HmctsCourtDataService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
) {

  fun getCourtAppearanceFromHmctsHearingId(courtHearingId: UUID): CourtAppearance {
    val hearing = courtDataIngestionApi.getCourtHearing(courtHearingId)

    return CourtAppearance(
      appearanceUuid = courtHearingId,
      outcome = null,
      courtCode = hearing.courtId.toString(), // TODO Get court code from registry?
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
        UploadedDocument(
          it.documentId,
          mapDocumentType(it.documentType),
          it.filename ?: "Unknown filename",
        )
      },
      source = EventSource.DPS,
    )
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
