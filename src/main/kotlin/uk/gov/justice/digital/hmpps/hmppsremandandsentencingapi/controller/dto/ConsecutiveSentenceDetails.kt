package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class ConsecutiveSentenceDetails(
  val sentenceUuid: UUID,
  val consecutiveToSentenceUuid: UUID?,

  // TODO also why arent we checking add journey for loops
  // TODO these are unused at the moment. May need to use them because the sentences
  //  from the UI mau not have UUID's when they are new sentences (I'm not sure need to check what the UI does)
  val sentenceReference: String? = null,
  val consecutiveToSentenceReference: String? = null,
)
