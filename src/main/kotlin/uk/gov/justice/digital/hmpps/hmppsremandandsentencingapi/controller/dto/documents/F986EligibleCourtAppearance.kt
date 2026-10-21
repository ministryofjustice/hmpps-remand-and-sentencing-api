package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import java.time.LocalDate
import java.util.UUID

data class F986EligibleCourtAppearance(
  val appearanceUuid: UUID,
  val warrantDate: LocalDate,
  val courtCode: String,
  val caseReference: String?,
  val offences: List<F986EligibleCourtAppearanceOffence>,
)
