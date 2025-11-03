package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate

data class LegacyLinkChargeToCase(
  val sourceCourtCaseUuid: String,
  val linkedDate: LocalDate,
  val performedByUser: String?,
)
