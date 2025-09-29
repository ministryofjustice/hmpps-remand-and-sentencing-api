package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository

@Service
class SentenceEnvelopeService(private val sentenceRepository: SentenceRepository)
