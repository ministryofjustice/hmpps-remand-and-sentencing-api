package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

import java.time.LocalDate

data class PrisonerSentenceEnvelope(
  val envelopeStartDate: LocalDate,
  val sentences: List<PrisonerSentenceEnvelopeSentence>,
) {
  companion object {
    fun from(sentenceChain: List<PrisonerSentenceEnvelopeSentence>): PrisonerSentenceEnvelope {
      val envelopeStartDate = sentenceChain.minOf { it.appearanceDate }
      return PrisonerSentenceEnvelope(
        envelopeStartDate,
        sentenceChain,
      )
    }
  }
}
