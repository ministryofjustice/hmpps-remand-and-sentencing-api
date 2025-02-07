package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.time.LocalDate
import java.util.UUID

@Service
class SentenceTypeService(private val sentenceTypeRepository: SentenceTypeRepository) {

  fun search(age: Int, convictionDate: LocalDate): List<SentenceType> = sentenceTypeRepository.findByAgeInAndConvictionDateInAndClassificationNot(
    age,
    convictionDate,
    SentenceTypeClassification.LEGACY_RECALL,
  ).map { SentenceType.from(it) }

  fun findByUuid(sentenceTypeUuid: UUID): SentenceType? = sentenceTypeRepository.findBySentenceTypeUuid(sentenceTypeUuid)?.let { SentenceType.from(it) }

  fun findByUuids(uuids: List<UUID>): List<SentenceType> = sentenceTypeRepository.findBySentenceTypeUuidIn(uuids).map { SentenceType.from(it) }
}
