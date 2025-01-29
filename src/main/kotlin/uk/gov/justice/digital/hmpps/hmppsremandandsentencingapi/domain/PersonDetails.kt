package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonerDetails
import java.time.LocalDate

data class PersonDetails(
  val personId: String,
  val firstName: String,
  val lastName: String,
  val establishment: String?,
  val cellNumber: String?,
  val dateOfBirth: LocalDate,
  val pncNumber: String?,
  val status: String?,
) {
  companion object {
    fun from(prisonerDetails: PrisonerDetails): PersonDetails = PersonDetails(
      prisonerDetails.offenderNo,
      prisonerDetails.firstName,
      prisonerDetails.lastName,
      prisonerDetails.assignedLivingUnit?.agencyName,
      prisonerDetails.assignedLivingUnit?.description,
      prisonerDetails.dateOfBirth,
      prisonerDetails.getPNCNumber(),
      prisonerDetails.legalStatus,
    )
  }
}
