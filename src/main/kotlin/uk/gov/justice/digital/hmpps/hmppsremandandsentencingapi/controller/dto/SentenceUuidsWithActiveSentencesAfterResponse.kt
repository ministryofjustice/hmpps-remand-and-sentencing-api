package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class SentenceUuidsWithActiveSentencesAfterResponse(
  val sentenceUuidsWithActiveSentencesAfter: List<UUID>,
)
