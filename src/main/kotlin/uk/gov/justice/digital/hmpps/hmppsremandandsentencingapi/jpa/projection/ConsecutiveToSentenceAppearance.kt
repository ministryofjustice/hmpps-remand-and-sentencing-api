package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.time.LocalDate
import java.util.UUID

data class ConsecutiveToSentenceAppearance(
  val appearanceUuid: UUID,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
)
