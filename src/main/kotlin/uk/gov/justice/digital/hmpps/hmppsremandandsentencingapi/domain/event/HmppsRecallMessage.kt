package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsRecallMessage(
  val recallId: String,
  val sentenceIds: List<String>,
  val previousSentenceIds: List<String>?,
  val previousRecallId: String?,
  val source: EventSource = EventSource.DPS,
)
