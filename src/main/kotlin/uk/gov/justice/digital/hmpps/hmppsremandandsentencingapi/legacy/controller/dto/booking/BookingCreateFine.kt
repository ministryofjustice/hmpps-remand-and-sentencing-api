package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import java.math.BigDecimal

data class BookingCreateFine(
  val fineAmount: BigDecimal,
)
