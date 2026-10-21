package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.time.LocalDate
import java.util.UUID

class CourtAppearanceWithFineRow(
  val appearanceUuid: UUID,
  val appearanceDate: LocalDate,
  val courtCode: String,
  val courtCaseReference: String?,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
)
