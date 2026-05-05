package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import java.time.LocalDate

interface PrisonerDetailsService {

  fun getPrisonerDetails(prisonerNumber: String, from: LocalDate? = null, to: LocalDate? = null): SarContent?
}
