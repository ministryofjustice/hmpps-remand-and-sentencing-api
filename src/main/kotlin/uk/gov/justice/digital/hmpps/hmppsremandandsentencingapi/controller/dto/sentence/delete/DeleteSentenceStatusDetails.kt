package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentence.delete

data class DeleteSentenceStatusDetails(
  val status: DeleteSentenceStatus,
  val reasons: List<DeleteSentenceStatusReason>,
)
