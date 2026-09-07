package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import java.time.LocalDate

data class SentenceOrderingKey(
  val countNumber: String?,
  val lineNumber: String?,
  val offenceStartDate: LocalDate?,
)
