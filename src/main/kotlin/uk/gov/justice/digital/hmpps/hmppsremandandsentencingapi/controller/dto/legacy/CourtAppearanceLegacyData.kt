package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy

data class CourtAppearanceLegacyData(
  val eventId: String?,
  val caseId: String?,
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
)
