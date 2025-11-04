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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.DeactivatedCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.DeactivatedSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergePerson
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeSentenceId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class PrisonerMergeDataCreator {
  companion object Factory {

    fun mergePerson(
      removedPrisonerNumber: String = "PRI123",
      performedByUser: String? = null,
      casesCreated: List<MergeCreateCourtCase> = listOf(mergeCreateCourtCase()),
      casesDeactivated: List<DeactivatedCourtCase> = listOf(),
      sentencesDeactivated: List<DeactivatedSentence> = listOf(),
    ): MergePerson = MergePerson(removedPrisonerNumber, performedByUser, casesCreated, casesDeactivated, sentencesDeactivated)

    fun deactivatedCourtCase(
      dpsCourtCaseUuid: String = "",
      active: Boolean = false,
    ): DeactivatedCourtCase = DeactivatedCourtCase(dpsCourtCaseUuid, active)

    fun deactivatedSentence(
      dpsSentenceUuid: UUID = UUID.randomUUID(),
      active: Boolean = false,
    ): DeactivatedSentence = DeactivatedSentence(dpsSentenceUuid, active)

    fun mergeCreateCourtCase(
      caseId: Long = 1,
      active: Boolean = true,
      courtCaseLegacyData: CourtCaseLegacyData = courtCaseLegacyData(),
      appearances: List<MergeCreateCourtAppearance> = listOf(
        mergeCreateCourtAppearance(),
      ),
      merged: Boolean = false,
    ): MergeCreateCourtCase = MergeCreateCourtCase(caseId, active, courtCaseLegacyData, appearances, merged)

    fun mergeCreateCourtAppearance(
      eventId: Long = 1,
      courtCode: String = "COURT1",
      appearanceDate: LocalDate = LocalDate.now(),
      appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"),
      legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData(),
      charges: List<MergeCreateCharge> = listOf(mergeCreateCharge()),
    ): MergeCreateCourtAppearance = MergeCreateCourtAppearance(eventId, courtCode, appearanceDate, appearanceTypeUuid, legacyData, charges)

    fun mergeCreateCharge(
      chargeNOMISId: Long = 5453,
      offenceCode: String = "OFF1",
      offenceStartDate: LocalDate? = LocalDate.now(),
      offenceEndDate: LocalDate? = null,
      legacyData: ChargeLegacyData = chargeLegacyData(),
      sentence: MergeCreateSentence? = mergeCreateSentence(),
      mergedFromCaseId: Long? = null,
      mergedFromDate: LocalDate? = null,
    ): MergeCreateCharge = MergeCreateCharge(
      chargeNOMISId,
      offenceCode,
      offenceStartDate,
      offenceEndDate,
      legacyData,
      sentence,
      mergedFromCaseId,
      mergedFromDate,
    )

    fun mergeCreateSentence(
      sentenceId: MergeSentenceId = mergeSentenceId(),
      fine: MergeCreateFine = mergeCreateFine(),
      active: Boolean = true,
      legacyData: SentenceLegacyData = sentenceLegacyData(),
      consecutiveToSentenceId: MergeSentenceId? = null,
      periodLengths: List<MergeCreatePeriodLength> = listOf(mergeCreatePeriodLength()),
      returnToCustodyDate: LocalDate? = null,
    ): MergeCreateSentence = MergeCreateSentence(
      sentenceId,
      fine,
      active,
      legacyData,
      consecutiveToSentenceId,
      periodLengths,
      returnToCustodyDate,
    )

    fun mergeCreatePeriodLength(
      periodLengthId: NomisPeriodLengthId = nomisPeriodLengthId(),
      periodYears: Int? = 2,
      periodMonths: Int? = null,
      periodWeeks: Int? = null,
      periodDays: Int? = 2,
      legacyData: PeriodLengthLegacyData = periodLengthLegacyData(),
    ): MergeCreatePeriodLength = MergeCreatePeriodLength(periodLengthId, periodYears, periodMonths, periodWeeks, periodDays, legacyData)

    fun mergeCreateFine(fineAmount: BigDecimal = BigDecimal.TEN): MergeCreateFine = MergeCreateFine(fineAmount)

    fun mergeSentenceId(offenderMergeId: Long = 1, sequence: Int = 1): MergeSentenceId = MergeSentenceId(offenderMergeId, sequence)
  }
}
