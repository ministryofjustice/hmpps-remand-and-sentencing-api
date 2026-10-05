package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsNextCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsResultKeys
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.NextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

@Component
class HmctsCourtDataService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val documentService: DocumentManagementApiClient,
  val chargeOutcomeService: ChargeOutcomeService,
  val appearanceOutcomeService: AppearanceOutcomeService,
  val sentenceTypeService: SentenceTypeService,
) {

  fun getCourtAppearanceFromHmctsHearingId(courtHearingId: UUID, prisonerNumber: String): CourtAppearance {
    val hearing = courtDataIngestionApi.getCourtHearing(courtHearingId, prisonerNumber)
    return getCourtAppearance(hearing)
  }

  private fun getCourtAppearance(hearing: HmctsCourtHearing): CourtAppearance {
    val documents = documentService.getDocumentsByIds(hearing.documents.map { it.documentId.toString() })
      .filter { it.duplicateOf == null }
    val chargeOutcomes = chargeOutcomeService.getAllByStatus(listOf(ReferenceEntityStatus.ACTIVE))
    val charges = hearing.charges.map { mapCharge(it, chargeOutcomes) }
    val chargeOutcomeIds = charges.mapNotNull { it.outcome?.outcomeUuid }

    val appearanceOutcome = if (chargeOutcomeIds.distinct().size == 1) {
      // All charge outcomes the same, find matching appearance outcome.
      val chargeOutcomeId = chargeOutcomeIds.first()
      val appearanceOutcomes = appearanceOutcomeService.getAllByStatus(listOf(ReferenceEntityStatus.ACTIVE))
      appearanceOutcomes.find { it.relatedChargeOutcomeUuid == chargeOutcomeId }
    } else {
      null
    }

    return CourtAppearance(
      appearanceUuid = UUID.randomUUID(),
      outcome = appearanceOutcome,
      courtCode = hearing.courtCode ?: Constants.nilUUID.toString(),
      courtCaseReference = hearing.caseReferences.firstOrNull(),
      criminalAppealOfficeReference = null,
      appearanceDate = hearing.hearingDate,
      warrantType = mapWarrantType(hearing),
      nextCourtAppearance = hearing.nextHearing?.let { mapNextCourtAppearance(it) },
      charges = charges,
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
      periodLengths = emptyList(),
      hmctsCourtHearingId = hearing.hearingId,
    )
  }

  private fun mapNextCourtAppearance(nextAppearance: HmctsNextCourtHearing): NextCourtAppearance? = NextCourtAppearance(
    appearanceDate = nextAppearance.hearingDate?.toLocalDate() ?: LocalDate.MIN,
    appearanceTime = nextAppearance.hearingDate?.toLocalTime() ?: LocalTime.MIN,
    courtCode = nextAppearance.hmppsCourtId ?: Constants.nilUUID.toString(),
    appearanceType = AppearanceType(
      appearanceTypeUuid = Constants.nilUUID,
      description = "Unknown appearance type",
      displayOrder = 1,
      hasSubtypes = false,
    ),
    futureSkeletonAppearanceUuid = Constants.nilUUID,
    courtAppearanceSubType = null,
    hmctsCourtHearingId = nextAppearance.hearingId,
  )

  private fun mapCharge(charge: HmctsCourtCharge, chargeOutcomes: List<ChargeOutcome>): Charge {
    val resultsWithMapping = charge.results.filter { result -> chargeOutcomes.find { result.code == it.hmctsCode } != null }

    val outcome = if (resultsWithMapping.size == 1) {
      chargeOutcomes.find { it.hmctsCode == resultsWithMapping.first().code }
    } else {
      null
    }
    return Charge(
      chargeUuid = UUID.randomUUID(),
      offenceCode = charge.code,
      offenceStartDate = charge.startDate,
      offenceEndDate = charge.endDate,
      outcome = outcome,
      aggravatingFactors = emptyList(),
      sentence = mapSentence(charge, resultsWithMapping),
      legacyData = null,
      mergedFromCase = null,
      createdAt = ZonedDateTime.now(),
      findingOfDomesticAbuse = null,
      hmctsChargeId = charge.chargeId,
    )
  }

  private fun mapSentence(charge: HmctsCourtCharge, resultsWithMapping: List<HmctsCourtResult>): Sentence? {
    if (resultsWithMapping.size != 1) {
      return null
    }

    val result = resultsWithMapping[0]
    // TODO remove logic to enable testing in dev where data is nonsensical.
//    val extensionPeriod = result.findValue(HmctsResultKeys.EXTENSION_PERIOD) != null
//    val extensionPeriod35 = result.findValue(HmctsResultKeys.EXTENSION_PERIOD_SECTION_35A) != null
//    val consecutive = result.findValue(HmctsResultKeys.CONSECUTIVE_TO_OFFENCE) != null ||
//      result.findValue(HmctsResultKeys.CONCURRENT) == "false"
//    val foreignPower = result.findValue(HmctsResultKeys.FOREIGN_POWER_SECTION_31) == "true"
//
//    if (extensionPeriod || extensionPeriod35 || consecutive || foreignPower) {
//      return null
//    }

    val imprisonmentPeriod = result.findValue(HmctsResultKeys.IMPRISONMENT_PERIOD)

    if (imprisonmentPeriod.isNullOrEmpty()) {
      return null
    }
    // currently only support concurrent standard sentences.

    val sentenceType = sentenceTypeService.findByUuid(UUID.fromString("02fe3513-40a6-47e9-a72d-9dafdd936a0e"))
    val duration = HmctsDurationParts.parseDuration(imprisonmentPeriod)

    return Sentence(
      sentenceUuid = UUID.randomUUID(),
      chargeNumber = null,
      periodLengths = listOf(
        PeriodLength(
          years = duration.years,
          months = duration.months,
          weeks = duration.weeks,
          days = duration.days,
          periodOrder = "years,months,weeks,days",
          periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
          legacyData = null,
          periodLengthUuid = UUID.randomUUID(),
        ),
      ),
      consecutiveToSentenceUuid = null,
      sentenceServeType = "CONCURRENT",
      sentenceType = sentenceType,
      convictionDate = charge.convictionDate,
      fineAmount = null,
      legacyData = null,
      hasRecall = false,
      status = SentenceEntityStatus.ACTIVE,
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
}
