package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.math.BigDecimal

data class CreateFineAmount(
  val fineAmount: BigDecimal,
)
