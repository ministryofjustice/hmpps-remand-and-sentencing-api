package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema

data class MigrationCreateCourtCase(
  val caseId: Long,
  val active: Boolean,
  val courtCaseLegacyData: CourtCaseLegacyData,
  val appearances: List<MigrationCreateCourtAppearance>,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:JsonSetter(nulls = Nulls.SKIP)
  var merged: Boolean = false,
)
