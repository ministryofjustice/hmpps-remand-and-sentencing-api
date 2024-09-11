package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.time.LocalDate

@Service
class SentenceTypeService(private val sentenceTypeRepository: SentenceTypeRepository) {
  fun search(age: Int, convictionDate: LocalDate): List<SentenceType> {
    return sentenceTypeRepository.findByAgeInAndConvictionDateIn(age, convictionDate).map { SentenceType.from(it) }
  }
}
