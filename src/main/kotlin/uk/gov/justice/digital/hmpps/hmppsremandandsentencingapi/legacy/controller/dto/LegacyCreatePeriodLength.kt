package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class LegacyCreatePeriodLength(
  val periodLengthId: NomisPeriodLengthId,
  val periodLengthUuid: UUID?,
  val periodYears: Int?,
  val periodMonths: Int?,
  val periodWeeks: Int?,
  val periodDays: Int?,
  val legacyData: PeriodLengthLegacyData,
)
