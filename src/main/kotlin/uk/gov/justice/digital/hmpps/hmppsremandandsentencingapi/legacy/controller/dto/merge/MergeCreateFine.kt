package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import java.math.BigDecimal

data class MergeCreateFine(
  val fineAmount: BigDecimal,
)
