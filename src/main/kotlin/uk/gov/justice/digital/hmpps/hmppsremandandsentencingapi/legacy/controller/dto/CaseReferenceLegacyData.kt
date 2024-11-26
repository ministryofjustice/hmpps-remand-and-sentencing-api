package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDateTime

data class CaseReferenceLegacyData(
  val offenderCaseReference: String,
  val updatedDate: LocalDateTime,
)
