package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

import com.fasterxml.jackson.annotation.JsonInclude

data class HmppsRecallMessage(
  val recallId: String,
  val sentenceIds: List<String>,
  val previousSentenceIds: List<String>?,
  val previousRecallId: String?,
  val source: EventSource = EventSource.DPS,
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  val nomisRecall: Boolean? = null, // Only gets set for NOMIS recalls (field is omitted for DPS recalls)
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  val sentenceDeleted: Boolean? = null, // Only gets set for NOMIS recalls (field is omitted for DPS recalls)
)
