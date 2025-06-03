package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class CourtCaseRow(
  var courtCaseId: Int,
  var prisonerId: String,
  var courtCaseUuid: String,
  var courtCaseStatus: EntityStatus,
  var courtCaseLegacyData: CourtCaseLegacyData?,
  var appearanceCount: Long,
  var caseReferences: String,
  var firstDayInCustody: LocalDate,
  var appearancePeriodLengthYears: Int?,
  var appearancePeriodLengthMonths: Int?,
  var appearancePeriodLengthWeeks: Int?,
  var appearancePeriodLengthDays: Int?,
  var appearancePeriodLengthOrder: String?,
  var appearancePeriodLengthType: PeriodLengthType?,
  var nextCourtAppearanceCourtCode: String?,
  var nextCourtAppearanceTypeDescription: String?,
  var nextCourtAppearanceDate: LocalDate?,
  var nextCourtAppearanceTime: LocalTime?,
  var latestCourtAppearanceCaseReference: String?,
  var latestCourtAppearanceCourtCode: String,
  var latestCourtAppearanceDate: LocalDate,
  var latestCourtAppearanceWarrantType: String,
  var latestCourtAppearanceOutcome: String?,
  var latestCourtAppearanceLegacyData: CourtAppearanceLegacyData?,
  var latestCourtAppearanceOverallConvictionDate: LocalDate?,
  var chargeId: Int?,
  var chargeStatus: EntityStatus?,
  var chargeOffenceCode: String?,
  var chargeOffenceStartDate: LocalDate?,
  var chargeOffenceEndDate: LocalDate?,
  var chargeOutcome: String?,
  var chargeLegacyData: ChargeLegacyData?,
  var sentenceId: Int?,
  var sentenceUuid: UUID?,
  var sentenceChargeNumber: String?,
  var sentenceStatus: EntityStatus?,
  var sentenceServeType: String?,
  var sentenceConvictionDate: LocalDate?,
  var sentenceLegacyData: SentenceLegacyData?,
  var sentenceFineAmount: BigDecimal?,
  var sentenceConsecutiveToUuid: UUID?,
  var sentenceTypeDescription: String?,
  var sentencePeriodLengthId: Int?,
  var sentencePeriodLengthStatus: EntityStatus?,
  var sentencePeriodLengthYears: Int?,
  var sentencePeriodLengthMonths: Int?,
  var sentencePeriodLengthWeeks: Int?,
  var sentencePeriodLengthDays: Int?,
  var sentencePeriodLengthOrder: String?,
  var sentencePeriodLengthType: PeriodLengthType?,
  var sentencePeriodLengthLegacyData: PeriodLengthLegacyData?,
)
