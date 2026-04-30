package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.AllDataPrisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Period
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Sentence
import java.time.LocalDate
import java.time.ZonedDateTime

object PrisonerTestData {

  fun prisoner(
    prisonerNumber: String = "A6764DZ",
    prisonerName: String = "RALPH DOG",
    courtCases: List<CourtCase> = listOf(courtCase()),
    recalls: List<Recall> = recalls(),
    immigrationDetentions: List<ImmigrationDetention> = immigrationDetentions(),
  ) = AllDataPrisoner(
    prisonerNumber = prisonerNumber,
    prisonerName = prisonerName,
    courtCases = courtCases,
    recalls = recalls,
    immigrationDetentions = immigrationDetentions,
  )

  fun courtCase() = CourtCase(
    courtName = "Glasgow High Court",
    caseStatus = "ACTIVE",
    createdAt = zoned("2026-02-03T10:02"),
    updatedAt = zoned("2026-02-03T10:02"),
    latestCourtAppearance = courtAppearance(),
  )

  fun courtAppearance() = CourtAppearance(
    appearanceDate = LocalDate.parse("2026-02-03"),
    appearanceOutcomeName = "Imprisonment",
    warrantType = "SENTENCING",
    convictionDate = null,
    nextAppearanceDate = null,
    charges = listOf(charge()),
  )

  fun charge() = Charge(
    offenceCode = "RF96124",
    offenceDescription = "A person procuring or persuading, or attempting to procure or persuade a reserve force naval rating or marine, liable",
    terrorRelated = null,
    foreignPowerRelated = null,
    domesticViolenceRelated = false,
    offenceStartDate = LocalDate.parse("1997-01-01"),
    offenceEndDate = null,
    chargeOutcome = "Imprisonment",
    sentences = listOf(sentence()),
  )

  fun sentence() = Sentence(
    sentenceType = "ORA Breach Top Up Supervision",
    sentenceServeType = "BOTUS",
    periods = listOf(Period(months = 6)),
    periodOrder = "CONCURRENT",
    isRecallable = false,
  )

  fun recalls() = listOf(
    Recall("LR", LocalDate.parse("2025-06-10"), null, true, "DELETED"),
    Recall("CUR_HDC", null, null, false, "ACTIVE"),
    Recall("LR", LocalDate.parse("2026-03-09"), null, true, "ACTIVE"),
  )

  fun immigrationDetentions() = listOf(
    ImmigrationDetention(
      "DEPORTATION_ORDER",
      "124222111",
      LocalDate.parse("2026-02-09"),
      null,
      null,
    ),
    ImmigrationDetention(
      "NO_LONGER_OF_INTEREST",
      null,
      LocalDate.parse("2026-03-01"),
      "RIGHT_TO_REMAIN",
      "",
    ),
  )

  private fun zoned(value: String) = ZonedDateTime.parse("$value+00:00[Europe/London]")
}
