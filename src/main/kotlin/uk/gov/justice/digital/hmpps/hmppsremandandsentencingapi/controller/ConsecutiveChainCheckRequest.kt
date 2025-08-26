package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ConsecutiveSentenceDetails
import java.util.UUID

data class ConsecutiveChainCheckRequest(
  val prisonerId: String,
  val appearanceUuid: UUID,
  val sourceSentenceUuid: UUID,
  val targetSentenceUuid: UUID,
  val sentences: List<ConsecutiveSentenceDetails>,
)
