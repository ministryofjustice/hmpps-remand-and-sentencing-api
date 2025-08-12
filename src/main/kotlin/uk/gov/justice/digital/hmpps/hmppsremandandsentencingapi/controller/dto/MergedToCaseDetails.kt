package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate

data class MergedToCaseDetails(
  val caseId: String?,
  val mergedToDate: LocalDate?,
  val caseReference: String?,
  val courtCode: String?,
  val warrantDate: LocalDate?,
)
