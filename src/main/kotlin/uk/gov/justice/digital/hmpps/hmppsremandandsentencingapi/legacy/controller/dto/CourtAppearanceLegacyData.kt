package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDateTime

data class CourtAppearanceLegacyData(
  val eventId: String?,
  val caseId: String?,
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
  val nextEventDateTime: LocalDateTime?,
)
