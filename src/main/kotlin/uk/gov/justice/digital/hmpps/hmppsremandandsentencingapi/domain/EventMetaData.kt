package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class EventMetaData(
  val prisonerId: String,
  val courtCaseId: String,
  val eventType: EventType,
)
