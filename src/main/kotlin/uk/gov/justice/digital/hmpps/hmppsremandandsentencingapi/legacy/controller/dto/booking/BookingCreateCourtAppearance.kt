package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate
import java.util.UUID

data class BookingCreateCourtAppearance(
  val eventId: Long,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val appearanceTypeUuid: UUID,
  val legacyData: CourtAppearanceLegacyData,
  val charges: List<BookingCreateCharge>,
)
