package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.chargeoutcome

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

data class CreateChargeOutcome(
  val outcomeUuid: UUID?,
  @param:NotBlank
  val outcomeName: String,
  @param:NotBlank
  val nomisCode: String,
  @param:NotBlank
  val outcomeType: String,
  @param:Positive
  val displayOrder: Int,
  @param:NotBlank
  val dispositionCode: String,
  val status: ReferenceEntityStatus,
)
