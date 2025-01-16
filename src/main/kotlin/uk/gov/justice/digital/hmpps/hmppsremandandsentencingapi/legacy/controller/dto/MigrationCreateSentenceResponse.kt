package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreateSentenceResponse(
  val lifetimeChargeUuid: UUID,
  val sentenceNOMISId: MigrationSentenceId,
)
