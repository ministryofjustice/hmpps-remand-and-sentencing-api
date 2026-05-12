package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import java.time.LocalDate

interface PrisonerDetailsService {

  fun getPrisonerDetails(prisonerNumber: String, from: LocalDate? = null, to: LocalDate? = null): SarContent?

  fun filterByDate(from: LocalDate?, to: LocalDate?, toCompare: LocalDate?): Boolean {
    if (from == null && to == null) return true
    if (toCompare == null) return false
    val fromOrEqual = from == null || toCompare.isAfter(from) || toCompare.isEqual(from)
    val toOrEqual = to == null || toCompare.isBefore(to) || toCompare.isEqual(to)
    return fromOrEqual && toOrEqual
  }
}
