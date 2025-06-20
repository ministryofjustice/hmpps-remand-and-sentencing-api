package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeIsValid
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.time.LocalDate
import java.util.UUID

@Service
class SentenceTypeService(
  private val sentenceTypeRepository: SentenceTypeRepository,
) {

  fun search(age: Int, convictionDate: LocalDate, statuses: List<ReferenceEntityStatus>, offenceDate: LocalDate): List<SentenceType> = sentenceTypeRepository.searchSentenceTypes(
    age,
    convictionDate,
    SentenceTypeClassification.LEGACY_RECALL,
    statuses,
    offenceDate,
  ).map { SentenceType.from(it) }

  fun findByUuid(sentenceTypeUuid: UUID): SentenceType? = sentenceTypeRepository.findBySentenceTypeUuid(sentenceTypeUuid)?.let { SentenceType.from(it) }

  fun findByUuids(uuids: List<UUID>): List<SentenceType> = sentenceTypeRepository.findBySentenceTypeUuidIn(uuids).map { SentenceType.from(it) }

  fun sentenceTypeIsStillValid(
    sentenceTypeUuid: UUID,
    age: Int,
    convictionDate: LocalDate,
    statuses: List<ReferenceEntityStatus>,
    offenceDate: LocalDate,
  ): SentenceTypeIsValid = SentenceTypeIsValid(sentenceTypeRepository.sentenceTypeStillValid(sentenceTypeUuid, age, convictionDate, statuses, offenceDate) != null)
}
