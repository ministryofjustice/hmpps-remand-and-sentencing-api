package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.util.UUID

data class SentenceCaseHierarchy(
  val prisonerId: String,
  val courtCaseUuid: String,
  val appearanceUuid: UUID,
  val chargeUuid: UUID,
  val sentenceUuid: UUID,
  val sentenceId: Int,
)
