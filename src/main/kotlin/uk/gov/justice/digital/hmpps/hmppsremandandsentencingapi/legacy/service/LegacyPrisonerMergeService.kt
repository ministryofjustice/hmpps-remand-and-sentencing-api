package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergePerson

@Service
class LegacyPrisonerMergeService(private val courtCaseRepository: CourtCaseRepository) {

  @Transactional
  fun process(mergePerson: MergePerson, retainedPrisonerNumber: String) {
    val courtCases = courtCaseRepository.findAllByPrisonerId(mergePerson.removedPrisonerNumber)
    courtCases.forEach { courtCase ->
      courtCase.prisonerId = retainedPrisonerNumber
    }
  }
}
