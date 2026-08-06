package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

data class ThingsToDo(
  val prisonerId: String,
  val thingsToDo: List<ThingToDo>,
)
data class ThingToDo(
  val type: ThingToDoType,
  val hearingThingsToDoData: HearingThingsToDoData,
)
enum class ThingToDoType {
  NEW_WARRANT,
}

data class HearingThingsToDoData(
  @Schema(description = "The ID of the hearing for this thing to do")
  val hearingId: UUID,
  @Schema(description = "The case reference of the hearing for this thing to do")
  val courtCaseReference: String,
  @Schema(description = "The date of the hearing for this thing to do")
  val hearingDate: LocalDate,
  @Schema(description = "The type of the hearing for this thing to do")
  val hearingType: String,
  @Schema(description = "The type of the hearing for this thing to do")
  val warrantType: HearingThingsToDoWarrantType,
  @Schema(description = "The ID of the existing court case for this warrant", nullable = true)
  val courtCaseUuid: String?,
)

enum class HearingThingsToDoWarrantType {
  REMAND,
  SENTENCING,
}
