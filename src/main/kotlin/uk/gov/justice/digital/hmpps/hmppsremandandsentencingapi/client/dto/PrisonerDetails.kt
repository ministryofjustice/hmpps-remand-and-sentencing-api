package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.time.LocalDate

data class PrisonerDetails(
  val offenderNo: String,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val assignedLivingUnit: AssignedLivingUnit?,
  val identifiers: List<PrisonerIdentifier>,
  val legalStatus: String?,
) {
  fun getPNCNumber(): String? = identifiers.firstOrNull { it.type == "PNC" }?.value
}
