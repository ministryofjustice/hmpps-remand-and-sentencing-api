package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import java.util.UUID

data class BookingCreatePeriodLengthResponse(
  val periodLengthUuid: UUID,
  val sentenceTermNOMISId: NomisPeriodLengthId,
)
