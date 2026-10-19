package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.unconfirmedsentencefix

import java.util.UUID

data class ConfirmConsecutiveToSentenceFixes(
  val sentenceUuids: List<UUID>,
)
