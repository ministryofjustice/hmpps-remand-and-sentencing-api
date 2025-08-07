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

  fun hasSentenceToChainTo(prisonerId: String, beforeOrOnAppearanceDate: LocalDate, bookingId: String?): HasSentenceToChainToResponse {
    return bookingId?.let {
      val countSentences = sentenceRepository.countConsecutiveToSentences(prisonerId, beforeOrOnAppearanceDate, it)
      return HasSentenceToChainToResponse(countSentences > 0)
    } ?: HasSentenceToChainToResponse(false)
  }

  @Transactional
  fun sentencesToChainTo(prisonerId: String, beforeOrOnAppearanceDate: LocalDate, bookingId: String?): RecordResponse<SentencesToChainToResponse> {
    return bookingId?.let {
      val consecutiveToSentenceUuids = sentenceRepository.findConsecutiveToSentences(prisonerId, beforeOrOnAppearanceDate, it).map { consecutiveToSentence -> consecutiveToSentence.toRecordEventMetadata(consecutiveToSentence.sentenceUuid) }
      val eventsToEmit = fixManyChargesToSentenceService.fixSentencesBySentenceUuids(consecutiveToSentenceUuids)
      return RecordResponse(SentencesToChainToResponse.from(sentenceRepository.findConsecutiveToSentences(prisonerId, beforeOrOnAppearanceDate, it)), eventsToEmit)
    } ?: RecordResponse(SentencesToChainToResponse.from(emptyList()), mutableSetOf())
  }
}
