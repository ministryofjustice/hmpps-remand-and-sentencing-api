package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.chargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.courtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.courtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.nomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.periodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.sentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingSentenceId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class BookingDataCreator {
  companion object Factory {
    fun bookingCreateCourtCases(
      prisonerId: String = "PRI123",
      courtCases: List<BookingCreateCourtCase> = listOf(bookingCreateCourtCase()),
    ): BookingCreateCourtCases = BookingCreateCourtCases(prisonerId, courtCases)

    fun bookingCreateCourtCase(
      caseId: Long = 1,
      active: Boolean = true,
      courtCaseLegacyData: CourtCaseLegacyData = courtCaseLegacyData(),
      appearances: List<BookingCreateCourtAppearance> = listOf(
        bookingCreateCourtAppearance(),
      ),
      merged: Boolean = false,
    ): BookingCreateCourtCase = BookingCreateCourtCase(caseId, active, courtCaseLegacyData, appearances, merged)

    fun bookingCreateCourtAppearance(
      eventId: Long = 1,
      courtCode: String = "COURT1",
      appearanceDate: LocalDate = LocalDate.now(),
      appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"),
      legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData(),
      charges: List<BookingCreateCharge> = listOf(bookingCreateCharge()),
    ): BookingCreateCourtAppearance = BookingCreateCourtAppearance(eventId, courtCode, appearanceDate, appearanceTypeUuid, legacyData, charges)

    fun bookingCreateCharge(
      chargeNOMISId: Long = 5453,
      offenceCode: String = "OFF1",
      offenceStartDate: LocalDate? = LocalDate.now(),
      offenceEndDate: LocalDate? = null,
      legacyData: ChargeLegacyData = chargeLegacyData(),
      sentence: BookingCreateSentence? = null,
      mergedFromCaseId: Long? = null,
      mergedFromDate: LocalDate? = null,
    ): BookingCreateCharge = BookingCreateCharge(
      chargeNOMISId,
      offenceCode,
      offenceStartDate,
      offenceEndDate,
      legacyData,
      sentence,
      mergedFromCaseId,
      mergedFromDate,
    )

    fun bookingCreateSentence(
      sentenceId: BookingSentenceId = bookingSentenceId(),
      fine: BookingCreateFine = bookingCreateFine(),
      active: Boolean = true,
      legacyData: SentenceLegacyData = sentenceLegacyData(),
      consecutiveToSentenceId: BookingSentenceId? = null,
      periodLengths: List<BookingCreatePeriodLength> = listOf(bookingCreatePeriodLength()),
      returnToCustodyDate: LocalDate? = null,
    ): BookingCreateSentence = BookingCreateSentence(
      sentenceId,
      fine,
      active,
      legacyData,
      consecutiveToSentenceId,
      periodLengths,
      returnToCustodyDate,
    )

    fun bookingCreatePeriodLength(
      periodLengthId: NomisPeriodLengthId = nomisPeriodLengthId(),
      periodYears: Int? = 2,
      periodMonths: Int? = null,
      periodWeeks: Int? = null,
      periodDays: Int? = 2,
      legacyData: PeriodLengthLegacyData = periodLengthLegacyData(),
    ): BookingCreatePeriodLength = BookingCreatePeriodLength(periodLengthId, periodYears, periodMonths, periodWeeks, periodDays, legacyData)

    fun bookingCreateFine(fineAmount: BigDecimal = BigDecimal.TEN): BookingCreateFine = BookingCreateFine(fineAmount)

    fun bookingSentenceId(offenderBookingId: Long = 1, sequence: Int = 1): BookingSentenceId = BookingSentenceId(offenderBookingId, sequence)
  }
}
