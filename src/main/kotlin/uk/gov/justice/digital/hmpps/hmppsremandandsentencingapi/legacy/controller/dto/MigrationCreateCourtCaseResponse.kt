package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationCreateCourtCaseResponse(
  val courtCaseUuid: String,
  val caseId: Long,
)
