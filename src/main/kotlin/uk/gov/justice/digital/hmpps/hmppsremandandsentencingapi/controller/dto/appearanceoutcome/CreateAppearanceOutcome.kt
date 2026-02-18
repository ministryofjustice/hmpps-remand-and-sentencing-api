package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.appearanceoutcome

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

data class CreateAppearanceOutcome(
  val outcomeUuid: UUID?,
  @field:NotBlank(message = "Outcome name must not be blank")
  val outcomeName: String,
  @field:NotBlank(message = "Nomis Code must not be blank")
  val nomisCode: String,
  @field:NotBlank(message = "Outcome type must not be blank")
  val outcomeType: String,
  @field:NotBlank(message = "Warrant type must not be blank")
  val warrantType: String,
  @field:Positive(message = "Display Order must be a whole positive number")
  val displayOrder: Int,
  @field:NotNull(message = "Related charge outcome uuid must not be blank")
  val relatedChargeOutcomeUuid: UUID,

  val isSubList: Boolean,
  @field:NotBlank(message = "Disposition Code must not be blank")
  val dispositionCode: String,
  val status: ReferenceEntityStatus,
)
