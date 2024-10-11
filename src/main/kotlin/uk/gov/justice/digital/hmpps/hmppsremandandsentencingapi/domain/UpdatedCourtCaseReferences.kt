package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import java.time.ZonedDateTime

data class UpdatedCourtCaseReferences(
  val prisonerId: String,
  val courtCaseId: String,
  val timeUpdated: ZonedDateTime,
  val hasUpdated: Boolean
)
