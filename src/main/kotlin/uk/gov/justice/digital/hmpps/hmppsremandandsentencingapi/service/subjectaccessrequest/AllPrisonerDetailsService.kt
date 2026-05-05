package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.CourtCaseSarRepository
import java.time.LocalDate

class AllPrisonerDetailsService(
  private val courtCaseSarRepository: CourtCaseSarRepository,
) : PrisonerDetailsService {
  override fun getPrisonerDetails(
    prisonerNumber: String,
    from: LocalDate?,
    to: LocalDate?,
  ): SarContent? {
    val foo = courtCaseSarRepository.findByPrisonerId(prisonerNumber)
    return null
  }
}
