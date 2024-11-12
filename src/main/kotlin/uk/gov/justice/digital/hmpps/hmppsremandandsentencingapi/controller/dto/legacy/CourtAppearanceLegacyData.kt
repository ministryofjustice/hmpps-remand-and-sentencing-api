package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy

import java.time.LocalDate
import java.time.LocalDateTime

data class CourtAppearanceLegacyData(
  val eventId: String?,
  val caseId: String?,
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
  val nextEventDate: LocalDate?,
  val nextEventStartTime: LocalDateTime?,
)
