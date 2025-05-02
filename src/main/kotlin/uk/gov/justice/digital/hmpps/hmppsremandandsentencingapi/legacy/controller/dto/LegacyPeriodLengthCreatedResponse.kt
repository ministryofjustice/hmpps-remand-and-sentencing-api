package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class LegacyPeriodLengthCreatedResponse(
  val periodLengthUuid: UUID,
  val prisonerId: String,
  val sentenceUuid: UUID,
  val chargeUuid: UUID,
  val appearanceUuid: UUID,
  val courtCaseId: String,
)
