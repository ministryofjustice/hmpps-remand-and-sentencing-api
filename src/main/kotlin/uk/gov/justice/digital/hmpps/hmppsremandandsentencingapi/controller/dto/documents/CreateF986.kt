package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import java.util.UUID

data class CreateF986(
  val courtAppearanceUuid: UUID,
  val courtPremise: String?,
  val courtStreet: String?,
  val courtTown: String?,
  val courtCounty: String?,
  val courtPostalCode: String?,
  val prisonTelephoneNumber: String?,
)
