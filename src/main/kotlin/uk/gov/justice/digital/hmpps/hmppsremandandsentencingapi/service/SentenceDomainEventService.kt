package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsSentenceMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import java.time.ZonedDateTime

@Service
class SentenceDomainEventService(
  private val snsService: SnsService,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${court.sentence.getByIdPath}") private val sentenceLookupPath: String,
) {
  fun create(prisonerId: String, sentenceId: String, courtChargeId: String, courtCaseId: String, courtAppearanceId: String, source: EventSource) {
    snsService.publishDomainEvent(
      "sentence.inserted",
      "Sentence inserted",
      generateDetailsUri(sentenceLookupPath, sentenceId),
      ZonedDateTime.now(),
      HmppsSentenceMessage(sentenceId, courtChargeId, courtCaseId, courtAppearanceId, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun update(prisonerId: String, sentenceId: String, courtChargeId: String, courtCaseId: String, courtAppearanceId: String, source: EventSource) {
    snsService.publishDomainEvent(
      "sentence.updated",
      "Sentence updated",
      generateDetailsUri(sentenceLookupPath, sentenceId),
      ZonedDateTime.now(),
      HmppsSentenceMessage(sentenceId, courtChargeId, courtCaseId, courtAppearanceId, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun delete(prisonerId: String, sentenceId: String, courtChargeId: String, courtCaseId: String, courtAppearanceId: String, source: EventSource) {
    snsService.publishDomainEvent(
      "sentence.deleted",
      "Sentence deleted",
      generateDetailsUri(sentenceLookupPath, sentenceId),
      ZonedDateTime.now(),
      HmppsSentenceMessage(sentenceId, courtChargeId, courtCaseId, courtAppearanceId, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
