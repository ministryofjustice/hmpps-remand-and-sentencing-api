package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreateSentenceResponse(
  val sentenceUuid: UUID,
  val sentenceNOMISId: MigrationSentenceId,
)
