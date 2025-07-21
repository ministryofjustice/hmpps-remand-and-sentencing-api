package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.time.LocalDate
import java.util.UUID

data class SentenceAfterOnAnotherCourtAppearanceRow(
  val appearanceUuid: UUID,
  val appearanceDate: LocalDate,
  val caseReference: String?,
  val courtCode: String,

)
