package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.math.BigDecimal

data class LegacyCreateFine(
  val fineAmount: BigDecimal,
)
