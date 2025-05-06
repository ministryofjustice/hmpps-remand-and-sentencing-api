package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import java.time.LocalDate

@Service
class ConsecutiveToSentenceService(private val sentenceRepository: SentenceRepository, private val fixManyChargesToSentenceService: FixManyChargesToSentenceService) {

  fun hasSentenceToChainTo(prisonerId: String, beforeOrOnAppearanceDate: LocalDate): HasSentenceToChainToResponse {
    val countSentences = sentenceRepository.countConsecutiveToSentences(prisonerId, beforeOrOnAppearanceDate)
    return HasSentenceToChainToResponse(countSentences > 0)
  }

  @Transactional
  fun sentencesToChainTo(prisonerId: String, beforeOrOnAppearanceDate: LocalDate): RecordResponse<SentencesToChainToResponse> {
    val consecutiveToSentences = sentenceRepository.findConsecutiveToSentences(prisonerId, beforeOrOnAppearanceDate)
    val eventsToEmit = fixManyChargesToSentenceService.fixSentences(consecutiveToSentences.map { it.toRecordEventMetadata(it.sentence) })
    return RecordResponse(SentencesToChainToResponse.from(consecutiveToSentences), eventsToEmit)
  }
}
