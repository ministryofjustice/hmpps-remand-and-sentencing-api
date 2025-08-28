package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import java.util.UUID

data class DeactivatedSentence(
  val dpsSentenceUuid: UUID,
  val active: Boolean,
)
