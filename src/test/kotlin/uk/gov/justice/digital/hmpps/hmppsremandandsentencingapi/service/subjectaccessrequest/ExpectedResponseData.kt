package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Sentence
import java.time.LocalDate
import java.time.ZonedDateTime

object ExpectedResponseData {

  fun expectedBasePrisonerDetails(prn: String): Prisoner = Prisoner(
    prisonerNumber = prn,
    prisonerName = null,
    courtCases = listOf(expectedBaseCourtCaseDetails()),
    recalls = listOf(expectedBaseRecallDetails()),
    immigrationDetentions = null,
  )

  fun expectedBaseCourtCaseDetails() = CourtCase(
    courtName = "Wandsworth Crown Court",
    caseStatus = "ACTIVE",
    createdAt = ZonedDateTime.parse("2026-05-07T10:00:00+00:00[Europe/London]"),
    updatedAt = ZonedDateTime.parse("2026-05-07T10:00:00+00:00[Europe/London]"),
    latestCourtAppearance = CourtAppearance(
      appearanceDate = LocalDate.of(2026, 3, 1),
      appearanceOutcomeName = "Imprisonment",
      warrantType = "SENTENCING",
      convictionDate = LocalDate.of(2026, 3, 6),
      nextAppearanceDate = null,
      charges = listOf(
        Charge(
          offenceCode = "RF96124",
          offenceDescription = "Littering",
          terrorRelated = false,
          foreignPowerRelated = false,
          domesticViolenceRelated = false,
          offenceStartDate = LocalDate.of(2026, 1, 1),
          offenceEndDate = LocalDate.of(2026, 1, 2),
          chargeOutcome = "Street Cleaning Order",
          liveSentence = Sentence(
            sentenceTypeDescription = "Detention and Training Order",
            sentenceTypeClassification = "DTO",
            periodLengths = listOf(
              PeriodLength(
                years = 0,
                months = 0,
                weeks = 3,
                days = 0,
                periodOrder = "years,months,weeks,days",
              ),
            ),
            sentenceServeType = "CONCURRENT",
          ),
        ),
      ),
    ),
    appearances = expectedCourtAppearances(),
  )

  fun expectedCourtAppearances() = listOf(
    CourtAppearance(
      appearanceDate = LocalDate.of(2026, 2, 3),
      appearanceOutcomeName = "Imprisonment",
      warrantType = "SENTENCING",
      convictionDate = LocalDate.of(2026, 2, 8),
      nextAppearanceDate = LocalDate.of(2026, 2, 3),
      charges = listOf(
        Charge(
          offenceCode = "RF96124",
          offenceDescription = "Littering",
          terrorRelated = false,
          foreignPowerRelated = false,
          domesticViolenceRelated = false,
          offenceStartDate = LocalDate.of(2026, 1, 1),
          offenceEndDate = LocalDate.of(2026, 1, 2),
          chargeOutcome = "Street Cleaning Order",
          liveSentence = Sentence(
            sentenceTypeDescription = "Detention and Training Order",
            sentenceTypeClassification = "DTO",
            periodLengths = listOf(
              PeriodLength(
                years = 0,
                months = 0,
                weeks = 3,
                days = 0,
                periodOrder = "years,months,weeks,days",
              ),
            ),
            sentenceServeType = "CONCURRENT",
          ),
        ),
      ),
    ),
    CourtAppearance(
      appearanceDate = LocalDate.of(2026, 3, 1),
      appearanceOutcomeName = "Imprisonment",
      warrantType = "SENTENCING",
      convictionDate = LocalDate.of(2026, 3, 6),
      nextAppearanceDate = null,
      charges = listOf(
        Charge(
          offenceCode = "RF96124",
          offenceDescription = "Littering",
          terrorRelated = false,
          foreignPowerRelated = false,
          domesticViolenceRelated = false,
          offenceStartDate = LocalDate.of(2026, 1, 1),
          offenceEndDate = LocalDate.of(2026, 1, 2),
          chargeOutcome = "Street Cleaning Order",
          liveSentence = Sentence(
            sentenceTypeDescription = "Detention and Training Order",
            sentenceTypeClassification = "DTO",
            periodLengths = listOf(
              PeriodLength(
                years = 0,
                months = 0,
                weeks = 3,
                days = 0,
                periodOrder = "years,months,weeks,days",
              ),
            ),
            sentenceServeType = "CONCURRENT",
          ),
        ),
      ),
    ),
  )

  fun expectedBaseRecallDetails() = Recall(
    recallType = "LR",
    revocationDate = LocalDate.of(2026, 6, 1),
    returnToCustodyDate = LocalDate.of(2026, 7, 2),
    inPrisonOnRevocationDate = false,
    recallSentenceStatus = "ACTIVE",
  )

  fun expectedBaseImmigrationDetentionDetails() = ImmigrationDetention(
    immigrationDetentionRecordType = "NO_LONGER_OF_INTEREST",
    homeOfficeReferenceNumber = "124222111",
    recordDate = LocalDate.of(2026, 6, 1),
    noLongerOfInterestReason = "RIGHT_TO_REMAIN",
    noLongerOfInterestComment = "Civilian awarded indefinite leave",
  )
}
