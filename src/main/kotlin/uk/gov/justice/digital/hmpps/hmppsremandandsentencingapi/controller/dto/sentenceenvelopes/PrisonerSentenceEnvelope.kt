package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

import java.time.LocalDate

data class PrisonerSentenceEnvelope(
  val envelopeStartDate: LocalDate,
  val sentences: List<PrisonerSentenceEnvelopeSentence>,
)
