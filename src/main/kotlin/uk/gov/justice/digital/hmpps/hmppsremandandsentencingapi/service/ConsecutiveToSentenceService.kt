package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import java.time.LocalDate

@Service
class ConsecutiveToSentenceService(private val sentenceRepository: SentenceRepository) {

  fun hasSentenceToChainTo(prisonerId: String, beforeOrOnAppearanceDate: LocalDate): HasSentenceToChainToResponse {
    val consecutiveToIds = sentenceRepository.findConsecutiveToIds(prisonerId)
    val countSentences = sentenceRepository.countConsecutiveToSentences(consecutiveToIds, prisonerId, beforeOrOnAppearanceDate)
    return HasSentenceToChainToResponse(countSentences > 0)
  }
}
