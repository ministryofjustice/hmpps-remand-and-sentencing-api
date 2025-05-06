package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsRecallMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import java.time.ZonedDateTime

@Service
class RecallDomainEventService(
  private val snsService: SnsService,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${recall.getByIdPath}") private val recallLookupPath: String,
) {
  fun create(prisonerId: String, recallId: String, sentenceIds: List<String>, source: EventSource) {
    snsService.publishDomainEvent(
      "recall.inserted",
      "Recall inserted",
      generateDetailsUri(recallLookupPath, recallId),
      ZonedDateTime.now(),
      HmppsRecallMessage(recallId, sentenceIds, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun update(prisonerId: String, recallId: String, sentenceIds: List<String>, source: EventSource) {
    snsService.publishDomainEvent(
      "recall.updated",
      "Recall updated",
      generateDetailsUri(recallLookupPath, recallId),
      ZonedDateTime.now(),
      HmppsRecallMessage(recallId, sentenceIds, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun delete(prisonerId: String, recallId: String, sentenceIds: List<String>, source: EventSource) {
    snsService.publishDomainEvent(
      "recall.deleted",
      "Recall deleted",
      generateDetailsUri(recallLookupPath, recallId),
      ZonedDateTime.now(),
      HmppsRecallMessage(recallId, sentenceIds, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
