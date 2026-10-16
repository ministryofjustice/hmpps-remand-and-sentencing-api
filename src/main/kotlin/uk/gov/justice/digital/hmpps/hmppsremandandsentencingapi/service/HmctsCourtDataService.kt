package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsNextCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsResultKeys
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.NextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class HmctsCourtDataService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val documentService: DocumentManagementApiClient,
  val chargeOutcomeService: ChargeOutcomeService,
  val appearanceOutcomeService: AppearanceOutcomeService,
  val sentenceTypeService: SentenceTypeService,
  val aggravatingFactorsService: AggravatingFactorsService,
) {

  fun getCourtAppearanceFromHmctsHearingId(courtHearingId: UUID, prisonerNumber: String): CourtAppearance {
    val hearing = courtDataIngestionApi.getCourtHearing(courtHearingId, prisonerNumber)
    return getCourtAppearance(hearing)
  }

  private fun getCourtAppearance(hearing: HmctsCourtHearing): CourtAppearance {
    val documents = documentService.getDocumentsByIds(hearing.documents.map { it.documentId.toString() })
      .filter { it.duplicateOf == null }
    val chargeOutcomes = chargeOutcomeService.getAllByStatus(listOf(ReferenceEntityStatus.ACTIVE))
    val aggravatingFactors = aggravatingFactorsService.getAllByStatuses(listOf(AggravatingFactorStatus.ACTIVE))
    var charges = hearing.charges.map { mapCharge(it, chargeOutcomes, aggravatingFactors) }
    charges = setFirstSentencedChargeToForthwith(charges)

    val appearanceOutcome = appearanceOutcome(charges)
    val periodLengths = overallPeriodLengths(hearing)

    return CourtAppearance(
      appearanceUuid = UUID.randomUUID(),
      outcome = appearanceOutcome,
      courtCode = hearing.courtCode ?: Constants.nilUUID.toString(),
      courtCaseReference = hearing.caseReferences.firstOrNull(),
      criminalAppealOfficeReference = null,
      appearanceDate = hearing.hearingDate,
      warrantType = mapWarrantType(hearing),
      nextCourtAppearance = hearing.nextHearing?.let { mapNextCourtAppearance(it, hearing.charges) },
      charges = charges,
      overallConvictionDate = overallConvictionDate(charges),
      legacyData = null,
      documents = mapDocuments(hearing, documents),
      source = EventSource.DPS,
      deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
      periodLengths = periodLengths,
      hmctsCourtHearingId = hearing.hearingId,
    )
  }

  private fun overallConvictionDate(charges: List<Charge>): LocalDate? {
    val convictionDates = charges.mapNotNull { it.sentence?.convictionDate }.distinct()
    if (convictionDates.size == 1) {
      return convictionDates.first()
    }
    return null
  }

  private fun setFirstSentencedChargeToForthwith(charges: List<Charge>): List<Charge> {
    val firstConcurrentCharge = charges.find { it.sentence?.sentenceServeType == "CONCURRENT" }
    return charges.map {
      if (it == firstConcurrentCharge) {
        it.copy(
          sentence = it.sentence!!.copy(
            sentenceServeType = "FORTHWITH",
          ),
        )
      } else {
        it
      }
    }
  }

  private fun mapDocuments(hearing: HmctsCourtHearing, documents: List<DocumentManagementApiDocument>) = hearing.documents.mapNotNull {
    documents.find { document -> document.documentUuid == it.documentId }
      ?.let { document ->
        UploadedDocument(
          it.documentId,
          mapDocumentType(it.documentType),
          document.documentFilename,
        )
      }
  }

  private fun appearanceOutcome(charges: List<Charge>): CourtAppearanceOutcome? {
    val chargeOutcomeIds = charges.mapNotNull { it.outcome?.outcomeUuid }
    return if (chargeOutcomeIds.distinct().size == 1) {
      // All charge outcomes the same, find matching appearance outcome.
      val chargeOutcomeId = chargeOutcomeIds.first()
      val appearanceOutcomes = appearanceOutcomeService.getAllByStatus(listOf(ReferenceEntityStatus.ACTIVE))
      appearanceOutcomes.find { it.relatedChargeOutcomeUuid == chargeOutcomeId }
    } else {
      null
    }
  }

  private fun overallPeriodLengths(hearing: HmctsCourtHearing): List<PeriodLength> {
    val overallLengthResult = hearing.charges.firstNotNullOfOrNull { charge -> charge.results.firstNotNullOfOrNull { result -> result.findValue(HmctsResultKeys.TOTAL_CUSTODIAL_PERIOD) } }
    return if (overallLengthResult != null) {
      val overallDuration = HmctsDurationParts.parseDuration(overallLengthResult)
      listOf(
        PeriodLength(
          years = overallDuration.years,
          months = overallDuration.months,
          weeks = overallDuration.weeks,
          days = overallDuration.days,
          periodOrder = "years,months,weeks,days",
          periodLengthType = PeriodLengthType.OVERALL_SENTENCE_LENGTH,
          legacyData = null,
          periodLengthUuid = UUID.randomUUID(),
        ),
      )
    } else {
      emptyList()
    }
  }

  private fun mapNextCourtAppearance(nextAppearance: HmctsNextCourtHearing, charges: List<HmctsCourtCharge>): NextCourtAppearance? {
    val hearingDate = nextAppearance.hearingDate?.toLocalDate() ?: return null
    val chargeLevelNextAppearanceDates = charges.flatMap { charge ->
      charge.results.filter { result -> NEXT_COURT_APPEARANCE_RESULT_CODES.contains(result.code) }
        .mapNotNull {
          val date = it.findValue(HmctsResultKeys.DATE_OF_HEARING) ?: return@mapNotNull null
          LocalDate.parse(date, PCR_DATE_FORMAT)
        }
    }.toMutableList()

    chargeLevelNextAppearanceDates += charges.flatMap { it.results }
      .mapNotNull { it.findValue(HmctsResultKeys.NEXT_HEARING_IN_MAGISTRATES_COURT) }
      .mapNotNull { NEXT_MAGISTRATE_REGEX.find(it)?.groupValues?.get(1) }
      .map { LocalDate.parse(it, PCR_DATE_FORMAT) }

    if (chargeLevelNextAppearanceDates.any { it != hearingDate }) {
      return null
    }
    return NextCourtAppearance(
      appearanceDate = nextAppearance.hearingDate.toLocalDate(),
      appearanceTime = nextAppearance.hearingDate.toLocalTime(),
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
  }

  private fun mapCharge(
    charge: HmctsCourtCharge,
    chargeOutcomes: List<ChargeOutcome>,
    aggravatingFactors: List<AggravatingFactor>,
  ): Charge {
    val resultsWithMapping = charge.results.filter { result -> chargeOutcomes.find { result.code == it.hmctsCode } != null }
    val factors = aggravatingFactors.filter { factor -> charge.results.find { it.code == factor.code } != null }
    val findingOfDomesticAbuse = charge.results.any { it.code == FINDING_OF_DOMESTIC_ABUSE_RESULT_CODE }
    val outcome = if (resultsWithMapping.size == 1) {
      chargeOutcomes.find { it.hmctsCode == resultsWithMapping.first().code }
    } else {
      null
    }
    val sentence = mapSentence(charge, resultsWithMapping)
    return Charge(
      chargeUuid = UUID.randomUUID(),
      offenceCode = charge.code,
      offenceStartDate = charge.startDate,
      offenceEndDate = charge.endDate,
      outcome = outcome,
      aggravatingFactors = factors,
      sentence = sentence,
      legacyData = null,
      mergedFromCase = null,
      createdAt = ZonedDateTime.now(),
      findingOfDomesticAbuse = if (sentence == null) null else findingOfDomesticAbuse,
      hmctsChargeId = charge.chargeId,
    )
  }

  private fun mapSentence(charge: HmctsCourtCharge, resultsWithMapping: List<HmctsCourtResult>): Sentence? {
    if (resultsWithMapping.size != 1) {
      return null
    }

    val periods = mutableListOf<PeriodLength>()
    val result = resultsWithMapping[0]
    val extensionPeriod = result.findValue(HmctsResultKeys.EXTENSION_PERIOD) != null
    val extensionPeriod35 = result.findValue(HmctsResultKeys.EXTENSION_PERIOD_SECTION_35A) != null
    val consecutive = result.findValue(HmctsResultKeys.CONSECUTIVE_TO_OFFENCE) != null ||
      result.findValue(HmctsResultKeys.CONCURRENT) == "false"
    val imprisonmentPeriod = result.findValue(HmctsResultKeys.IMPRISONMENT_PERIOD)

    if (!result.hasAnySentencingKey()) {
      return null
    }

    val sentenceType = if (extensionPeriod || extensionPeriod35) {
      null
    } else {
      sentenceTypeService.findByUuid(UUID.fromString("02fe3513-40a6-47e9-a72d-9dafdd936a0e"))
    }

    if (!imprisonmentPeriod.isNullOrEmpty()) {
      val duration = HmctsDurationParts.parseDuration(imprisonmentPeriod)
      periods.add(
        PeriodLength(
          years = duration.years,
          months = duration.months,
          weeks = duration.weeks,
          days = duration.days,
          periodOrder = "years,months,weeks,days",
          periodLengthType = if (!extensionPeriod && !extensionPeriod35) PeriodLengthType.SENTENCE_LENGTH else PeriodLengthType.CUSTODIAL_TERM,
          legacyData = null,
          periodLengthUuid = UUID.randomUUID(),
        ),
      )
    }

    return Sentence(
      sentenceUuid = UUID.randomUUID(),
      chargeNumber = null,
      periodLengths = periods,
      consecutiveToSentenceUuid = null,
      sentenceServeType = if (consecutive) "CONSECUTIVE" else "CONCURRENT",
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
  companion object {
    private const val FINDING_OF_DOMESTIC_ABUSE_RESULT_CODE = "FODA"
    private val NEXT_COURT_APPEARANCE_RESULT_CODES = listOf("NHMC", "NHCSSS")
    val PCR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val NEXT_MAGISTRATE_REGEX = Regex("""Date of hearing:\s*(\d{2}/\d{2}/\d{4})""")
  }
}
