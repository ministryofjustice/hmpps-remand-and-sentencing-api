package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import java.util.UUID

data class MergeCreateSentenceResponse(
  val sentenceUuid: UUID,
  val sentenceNOMISId: MergeSentenceId,
)
