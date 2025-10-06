package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes.PrisonerSentenceEnvelopeSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes.PrisonerSentenceEnvelopes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository

@Service
class SentenceEnvelopeService(private val sentenceRepository: SentenceRepository) {

  fun findByPrisonerId(prisonerId: String): PrisonerSentenceEnvelopes {
    val viewSentenceRows = sentenceRepository.findViewSentences(prisonerId).groupBy { it.sentenceUuid }
    val sentences = viewSentenceRows.map { PrisonerSentenceEnvelopeSentence.from(it) }
    val startOfChainSentences = sentences.filter { it.consecutiveToSentenceUuid == null }

    val sentenceEnvelopes = startOfChainSentences.map { startOfChain ->
      val chain = mutableListOf(startOfChain)
      val endOfChain = mutableListOf(startOfChain)
      while (endOfChain.isNotEmpty()) {
        val endOfChainSentence = endOfChain.removeFirst()
        val nextSentences = sentences.filter { it.consecutiveToSentenceUuid == endOfChainSentence.sentenceUuid && chain.none { chainSentence -> chainSentence.sentenceUuid == it.sentenceUuid } }
        chain.addAll(nextSentences)
        endOfChain.addAll(nextSentences)
      }
      chain
    }
    return PrisonerSentenceEnvelopes.from(sentenceEnvelopes)
  }
}
