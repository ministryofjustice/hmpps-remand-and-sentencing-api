package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import java.util.UUID

data class MergeCreatePeriodLengthResponse(
  val periodLengthUuid: UUID,
  val sentenceTermNOMISId: NomisPeriodLengthId,
)
