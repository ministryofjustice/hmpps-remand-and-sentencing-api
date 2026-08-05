package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentence.delete

data class DeleteSentenceStatusReasonDetails(
  val reason: DeleteSentenceStatusReason,
  val metadata: Map<String, Any>,
) {
  companion object {
    fun from(reason: DeleteSentenceStatusReason, metadata: Map<String, Any> = emptyMap()): DeleteSentenceStatusReasonDetails = DeleteSentenceStatusReasonDetails(reason, metadata)
  }
}
