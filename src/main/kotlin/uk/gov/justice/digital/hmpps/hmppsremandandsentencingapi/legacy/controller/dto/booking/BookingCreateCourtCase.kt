package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData

data class BookingCreateCourtCase(
  val caseId: Long,
  val active: Boolean,
  val courtCaseLegacyData: CourtCaseLegacyData,
  val appearances: List<BookingCreateCourtAppearance>,
  @param:Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @param:JsonSetter(nulls = Nulls.SKIP)
  var merged: Boolean = false,
)
