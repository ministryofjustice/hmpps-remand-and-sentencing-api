package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

data class CreateLodgeWarrants986(
  val prisonerId: String,
  val courtName: String,
  val courtPremise: String,
  val courtStreet: String,
  val courtTown: String,
  val courtCounty: String,
  val courtPostalCode: String,
  val prisonTelephoneNumber: String,
)
