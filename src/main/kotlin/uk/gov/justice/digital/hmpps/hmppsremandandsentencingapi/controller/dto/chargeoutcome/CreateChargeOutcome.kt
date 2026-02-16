package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.chargeoutcome

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

data class CreateChargeOutcome(
  val outcomeUuid: UUID?,
  @field:NotBlank(message = "Outcome name must not be blank")
  val outcomeName: String,
  @field:NotBlank(message = "Nomis Code must not be blank")
  val nomisCode: String,
  @field:NotBlank(message = "Outcome type must not be blank")
  val outcomeType: String,
  @field:Positive(message = "Display Order must be a whole positive number")
  val displayOrder: Int,
  @field:NotBlank(message = "Disposition Code must not be blank")
  val dispositionCode: String,
  val status: ReferenceEntityStatus,
)
