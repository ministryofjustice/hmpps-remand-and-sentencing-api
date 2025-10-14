package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ViewSentenceRow(
  val sentenceUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val dpsOffenceOutcome: String?,
  val countNumber: String?,
  val sentenceLegacyData: SentenceLegacyData?,
  val convictionDate: LocalDate?,
  val sentenceServeType: String,
  val consecutiveToSentenceUuid: UUID?,
  val fineAmount: BigDecimal?,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val chargeLegacyData: ChargeLegacyData?,
  val dpsSentenceType: String?,
  val periodLengthUuid: UUID?,
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String?,
  val periodLengthType: PeriodLengthType?,
  val periodLengthLegacyData: PeriodLengthLegacyData?,
  val mergedFromAppearanceId: Int?,
  val mergedFromCaseReference: String?,
  val mergedFromCourtCode: String?,
  val mergedFromWarrantDate: LocalDate?,
  val mergedFromDate: LocalDate?,
)
