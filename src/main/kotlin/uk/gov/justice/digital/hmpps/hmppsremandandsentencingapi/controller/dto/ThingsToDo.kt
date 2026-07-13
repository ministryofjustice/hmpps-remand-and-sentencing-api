package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ThingsToDo(
  val prisonerId: String,
  val thingsToDo: List<ThingToDoType> = emptyList(),
  val hearingThingsToDoData: HearingThingsToDoData?,
)

enum class ThingToDoType {
  NEW_REMAND_WARRANT,
  NEW_SENTENCING_WARRANT,
}

data class HearingThingsToDoData(
  @Schema(description = "The ID of the hearing for this thing to do", nullable = true)
  val hearingId: UUID,
  @Schema(description = "The case reference of the hearing for this thing to do", nullable = true)
  val courtCaseReference: String,
)
