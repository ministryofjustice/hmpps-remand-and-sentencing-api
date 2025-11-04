package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import java.time.LocalDate

fun doesRecallRequireUAL(
  revocationDate: LocalDate,
  returnToCustodyDate: LocalDate?,
): Boolean = if (returnToCustodyDate == null) {
  false
} else if (revocationDate.plusDays(1).isBefore(returnToCustodyDate)) {
  true
} else {
  false
}
