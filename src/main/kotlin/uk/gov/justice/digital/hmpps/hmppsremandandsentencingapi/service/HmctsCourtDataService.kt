package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsNextCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.NextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import java.time.ZonedDateTime
import java.util.UUID

@Component
class HmctsCourtDataService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val documentService: DocumentManagementApiClient,
  val courtRegisterApiClient: CourtRegisterApiClient,
  val chargeOutcomeService: ChargeOutcomeService,
) {

  fun getCourtAppearanceFromHmctsHearingId(courtHearingId: UUID, prisonerNumber: String): CourtAppearance {
    val hearing = courtDataIngestionApi.getCourtHearing(courtHearingId, prisonerNumber)
    return getCourtAppearance(hearing)
  }

  private fun getCourtAppearance(hearing: HmctsCourtHearing): CourtAppearance {
    val documents = documentService.getDocumentsByIds(hearing.documents.map { it.documentId.toString() })
      .filter { it.duplicateOf == null }
    val court = courtRegisterApiClient.getCourtRegisterByHmctsId(hearing.courtId)

    return CourtAppearance(
      appearanceUuid = hearing.hearingId,
      outcome = null,
      courtCode = court?.courtId ?: hearing.courtId.toString(),
      courtCaseReference = hearing.caseReferences.firstOrNull(),
      criminalAppealOfficeReference = null,
      appearanceDate = hearing.hearingDate,
      warrantType = mapWarrantType(hearing),
      nextCourtAppearance = hearing.nextHearing?.let { mapNextCourtAppearance(it) },
      charges = hearing.charges.map { mapCharge(it) },
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
      emptyList(),
    )
  }

  private fun mapNextCourtAppearance(nextAppearance: HmctsNextCourtHearing): NextCourtAppearance? = NextCourtAppearance(
    appearanceDate = nextAppearance.hearingDate.toLocalDate(),
    appearanceTime = nextAppearance.hearingDate.toLocalTime(),
    courtCode = nextAppearance.hmppsCourtId ?: nextAppearance.hmppsCourtId.toString(),
    appearanceType = AppearanceType(
      appearanceTypeUuid = Constants.nilUUID,
      description = "Unknown appearance type",
      displayOrder = 1,
      hasSubtypes = false,
    ),
    futureSkeletonAppearanceUuid = Constants.nilUUID,
    courtAppearanceSubType = null,
  )

  private fun mapCharge(charge: HmctsCourtCharge): Charge {
    val outcomeId = mapCodeToOutcome(charge.results.first().code)
    val outcome = outcomeId?.let { chargeOutcomeService.findByUuid(it) }
    return Charge(
      chargeUuid = UUID.randomUUID(),
      offenceCode = charge.code,
      offenceStartDate = charge.startDate,
      offenceEndDate = charge.endDate,
      outcome = outcome,
      aggravatingFactors = emptyList(),
      sentence = null,
      legacyData = null,
      mergedFromCase = null,
      createdAt = ZonedDateTime.now(),
    )
  }

  private fun mapDocumentType(documentType: String): String = if (documentType == "PRISON_COURT_REGISTER") {
    "PRISON_COURT_REGISTER"
  } else {
    "HMCTS_WARRANT"
  }

  private fun mapWarrantType(hearing: HmctsCourtHearing): String = if (hearing.documents.any { it.documentType == "SENTENCING_WARRANT" }) {
    "SENTENCING"
  } else {
    "NON_SENTENCING"
  }

  private fun mapCodeToOutcome(code: String): UUID? = when (code) {
    "RIB" -> UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8")
    else -> null
  }
}
