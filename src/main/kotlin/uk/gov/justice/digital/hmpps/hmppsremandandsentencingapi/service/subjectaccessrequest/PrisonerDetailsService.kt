package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import java.time.LocalDate

interface PrisonerDetailsService<T> {

  fun getPrisonerDetails(prisonerNumber: String, from: LocalDate?, to: LocalDate?): T?
}
