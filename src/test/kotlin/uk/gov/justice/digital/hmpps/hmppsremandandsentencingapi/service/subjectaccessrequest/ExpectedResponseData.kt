package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Sentence
import java.time.LocalDate
import java.time.ZonedDateTime

object ExpectedResponseData {

  fun expectedBasePrisonerDetails(prn: String): Prisoner = Prisoner(
    prisonerNumber = prn,
    prisonerName = null,
    courtCases = listOf(expectedBaseCourtCaseDetails()),
    recalls = null,
    immigrationDetentions = null,
  )

  fun expectedBaseCourtCaseDetails() = CourtCase(
    courtName = "",
    caseStatus = "ACTIVE",
    createdAt = ZonedDateTime.parse("2026-05-07T10:00:00+00:00[Europe/London]"),
    updatedAt = ZonedDateTime.parse("2026-05-07T10:00:00+00:00[Europe/London]"),
    latestCourtAppearance = CourtAppearance(
      appearanceDate = LocalDate.of(2026, 2, 3),
      appearanceOutcomeName = "Imprisonment",
      warrantType = "SENTENCING",
      convictionDate = LocalDate.of(2026, 5, 5),
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
    appearances = listOf(
      CourtAppearance(
        appearanceDate = LocalDate.of(2026, 2, 3),
        appearanceOutcomeName = "Imprisonment",
        warrantType = "SENTENCING",
        convictionDate = LocalDate.of(2026, 5, 5),
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
    ),
  )
}
