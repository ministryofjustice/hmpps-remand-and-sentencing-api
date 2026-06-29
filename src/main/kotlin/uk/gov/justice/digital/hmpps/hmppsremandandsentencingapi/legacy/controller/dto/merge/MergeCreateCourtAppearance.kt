package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate

data class MergeCreateCourtAppearance(
  val eventId: Long,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val legacyData: CourtAppearanceLegacyData,
  val charges: List<MergeCreateCharge>,
)
