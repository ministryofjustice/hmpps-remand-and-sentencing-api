package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreatePeriodLengthResponse(
  val uuid: UUID,
  val sentenceTermNOMISId: NomisPeriodLengthId,
)
