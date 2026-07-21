package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.util.UUID

data class LinkBreachSentence(
  val courtCaseUuid: String,
  val appearanceUuid: UUID,
  val chargeUuid: UUID,
  val sentenceUuid: UUID,
  val sentenceId: Int,
)
