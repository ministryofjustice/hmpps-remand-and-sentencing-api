package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.util.UUID

data class CreatePeriodLength(
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonSetter(nulls = Nulls.SKIP)
  var periodLengthUuid: UUID = UUID.randomUUID(),
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String,
  val type: PeriodLengthType,
  val prisonId: String,
)
