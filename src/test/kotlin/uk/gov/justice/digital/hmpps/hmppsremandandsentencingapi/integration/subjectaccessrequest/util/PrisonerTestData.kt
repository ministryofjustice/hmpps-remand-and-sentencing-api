package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner

object PrisonerTestData {

  fun prisoner(
    prisonerNumber: String = "A6764DZ",
    prisonerName: String = "RALPH DOG",
    immigrationDetentions: List<ImmigrationDetention> = immigrationDetentions(),
  ) = Prisoner(
    prisonerNumber = prisonerNumber,
    prisonerName = prisonerName,
    immigrationDetentions = immigrationDetentions,
  )

  fun immigrationDetentions() = listOf(
    ImmigrationDetention(
      homeOfficeReferenceNumber = "124222111",
      noLongerOfInterestReason = null,
      noLongerOfInterestComment = null,
    ),
    ImmigrationDetention(
      homeOfficeReferenceNumber = null,
      noLongerOfInterestReason = "RIGHT_TO_REMAIN",
      noLongerOfInterestComment = "",
    ),
  )
}
