package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

data class PrisonerSentenceEnvelopes(
  val sentenceEnvelopes: List<PrisonerSentenceEnvelope>,
) {
  companion object {
    fun from(envelopes: List<List<PrisonerSentenceEnvelopeSentence>>): PrisonerSentenceEnvelopes = PrisonerSentenceEnvelopes(
      envelopes.map {
        PrisonerSentenceEnvelope.from(it)
      },
    )
  }
}
