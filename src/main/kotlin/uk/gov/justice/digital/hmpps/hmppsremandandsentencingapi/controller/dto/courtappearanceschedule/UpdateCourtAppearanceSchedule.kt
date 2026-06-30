package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule

import java.time.LocalDateTime

data class UpdateCourtAppearanceSchedule(
  val courtCode: String,
  val reasonCode: String,
  val start: LocalDateTime,
  val comments: String?,
  val prisonCode: String,
)
