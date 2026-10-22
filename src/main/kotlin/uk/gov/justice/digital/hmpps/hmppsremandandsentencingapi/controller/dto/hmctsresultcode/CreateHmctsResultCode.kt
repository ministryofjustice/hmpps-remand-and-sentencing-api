package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

data class CreateHmctsResultCode(
  val resultUuid: UUID?,
  @field:NotBlank(message = "Result name must not be blank")
  val resultName: String,
  @field:NotBlank(message = "Result Code must not be blank")
  val resultCode: String,
  @field:NotNull(message = "Outcome UUID must not be blank")
  val outcomeUuid: UUID,
  val status: ReferenceEntityStatus,
)
