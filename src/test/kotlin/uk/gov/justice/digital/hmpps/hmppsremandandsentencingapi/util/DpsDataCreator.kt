package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateFineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DpsDataCreator {
  companion object Factory {
    const val DEFAULT_PRISONER_ID = "PRI123"
    fun dpsCreateCourtCase(
      prisonerId: String = DEFAULT_PRISONER_ID,
      appearances: List<CreateCourtAppearance> = listOf(dpsCreateCourtAppearance()),
      prisonId: String = "PRISON1",
    ): CreateCourtCase = CreateCourtCase(prisonerId, prisonId, appearances, null)

    fun dpsCreatePeriodLength(
      periodLengthUuid: UUID = UUID.randomUUID(),
      years: Int? = 1,
      months: Int? = null,
      weeks: Int? = null,
      days: Int? = null,
      periodOrder: String = "years",
      type: PeriodLengthType = PeriodLengthType.OVERALL_SENTENCE_LENGTH,
      prisonId: String = "PRISON1",
      legacyData: PeriodLengthLegacyData? = null,
    ): CreatePeriodLength = CreatePeriodLength(periodLengthUuid, years, months, weeks, days, periodOrder, type, prisonId, legacyData)

    fun dpsCreateNextCourtAppearance(
      appearanceDate: LocalDate = LocalDate.now().plusDays(2),
      appearanceTime: LocalTime? = LocalTime.of(10, 15, 0, 0),
      courtCode: String = "COURT1",
      appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"),
      prisonId: String = "PRISON1",
    ): CreateNextCourtAppearance = CreateNextCourtAppearance(appearanceDate, appearanceTime, courtCode, appearanceTypeUuid, prisonId)

    fun dpsCreateCourtAppearance(
      courtCaseUuid: String? = null,
      appearanceUUID: UUID = UUID.randomUUID(),
      outcomeUuid: UUID = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
      courtCode: String = "COURT1",
      courtCaseReference: String? = "GH123456789",
      appearanceDate: LocalDate = LocalDate.now(),
      warrantId: String? = "123",
      warrantType: String = "SENTENCING",
      overallSentenceLength: CreatePeriodLength? = dpsCreatePeriodLength(),
      nextCourtAppearance: CreateNextCourtAppearance? = dpsCreateNextCourtAppearance(),
      charges: List<CreateCharge> = listOf(dpsCreateCharge()),
      overallConvictionDate: LocalDate? = LocalDate.now().minusDays(7),
      legacyData: CourtAppearanceLegacyData? = null,
      prisonId: String = "PRISON1",
      documents: List<UploadedDocument> = listOf(dpsCreateUploadedDocument()),
    ): CreateCourtAppearance = CreateCourtAppearance(
      courtCaseUuid,
      appearanceUUID,
      outcomeUuid,
      courtCode,
      courtCaseReference,
      appearanceDate,
      warrantId,
      warrantType,
      overallSentenceLength,
      nextCourtAppearance,
      charges,
      overallConvictionDate,
      legacyData,
      prisonId,
      documents,
    )

    fun dpsCreateCharge(
      appearanceUuid: UUID? = null,
      chargeUuid: UUID = UUID.randomUUID(),
      offenceCode: String = "OFF123",
      offenceStartDate: LocalDate = LocalDate.now(),
      offenceEndDate: LocalDate? = null,
      outcomeUuid: UUID? = UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"),
      terrorRelated: Boolean? = true,
      sentence: CreateSentence? = dpsCreateSentence(),
      legacyData: ChargeLegacyData? = null,
      prisonId: String = "PRISON1",
    ): CreateCharge = CreateCharge(
      appearanceUuid,
      chargeUuid,
      offenceCode,
      offenceStartDate,
      offenceEndDate,
      outcomeUuid,
      terrorRelated,
      sentence,
      legacyData,
      prisonId,
    )

    fun dpsCreateSentence(
      sentenceUuid: UUID? = UUID.randomUUID(),
      chargeNumber: String = "1",
      periodLengths: List<CreatePeriodLength> = listOf(dpsCreatePeriodLength(type = PeriodLengthType.SENTENCE_LENGTH)),
      sentenceServeType: String = "FORTHWITH",
      consecutiveToSentenceReference: String? = null,
      consecutiveToSentenceUuid: UUID? = null,
      sentenceTypeId: UUID? = UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"),
      convictionDate: LocalDate? = LocalDate.now().minusDays(7),
      fineAmount: CreateFineAmount? = null,
      prisonId: String = "PRISON1",
      sentenceReference: String = "0",
    ): CreateSentence = CreateSentence(
      sentenceUuid,
      chargeNumber,
      periodLengths,
      sentenceServeType,
      consecutiveToSentenceUuid,
      sentenceTypeId,
      convictionDate,
      fineAmount,
      prisonId,
      sentenceReference,
      consecutiveToSentenceReference,
    )

    fun dpsCreateRecall(
      prisonerId: String = DpsDataCreator.DEFAULT_PRISONER_ID,
      revocationDate: LocalDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate: LocalDate = LocalDate.of(2024, 2, 3),
      inPrisonOnRevocationDate: Boolean? = null,
      recallTypeCode: RecallType = FTR_14,
      createdByUsername: String = "user001",
      createdByPrison: String = "PRISON1",
      sentenceIds: List<UUID> = listOf(UUID.randomUUID()),
    ): CreateRecall = CreateRecall(
      prisonerId,
      revocationDate,
      returnToCustodyDate,
      inPrisonOnRevocationDate,
      recallTypeCode,
      createdByUsername,
      createdByPrison,
      sentenceIds,
    )

    fun dpsCreateUploadedDocument(
      documentUuid: UUID = UUID.randomUUID(),
      documentType: String = "COURT_APPEARANCE",
      documentName: String = "document.pdf",
    ): UploadedDocument = UploadedDocument(documentUuid, documentType, documentName)
  }
}
