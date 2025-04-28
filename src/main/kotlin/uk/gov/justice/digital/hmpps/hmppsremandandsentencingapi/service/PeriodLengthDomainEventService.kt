package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsPeriodLengthMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import java.time.ZonedDateTime

@Service
class PeriodLengthDomainEventService(
  private val snsService: SnsService,
  @Value("\${ingress.url}") private val ingressUrl: String,
  // TODO this lookup path doesnt exist  in an an endpoint yet. will be created by another ticket then this TODO can be resolved
  @Value("\${period.length.getByIdPath}") private val periodLengthLookupPath: String,
) {

  fun create(
    prisonerId: String,
    periodLengthId: String,
    sentenceId: String,
    courtChargeId: String,
    courtCaseId: String,
    courtAppearanceId: String,
    source: EventSource,
  ) {
    snsService.publishDomainEvent(
      "sentence.period-length.inserted",
      "Sentence period length inserted",
      generateDetailsUri(periodLengthLookupPath, periodLengthId),
      ZonedDateTime.now(),
      HmppsPeriodLengthMessage(
        periodLengthId = periodLengthId,
        sentenceId = sentenceId,
        courtChargeId = courtChargeId,
        courtCaseId = courtCaseId,
        courtAppearanceId = courtAppearanceId,
        source = source,
      ),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun update(
    prisonerId: String,
    periodLengthId: String,
    sentenceId: String,
    courtChargeId: String,
    courtCaseId: String,
    courtAppearanceId: String,
    source: EventSource,
  ) {
    snsService.publishDomainEvent(
      "sentence.period-length.updated",
      "Sentence period length updated",
      generateDetailsUri(periodLengthLookupPath, periodLengthId),
      ZonedDateTime.now(),
      HmppsPeriodLengthMessage(
        periodLengthId = periodLengthId,
        sentenceId = sentenceId,
        courtChargeId = courtChargeId,
        courtCaseId = courtCaseId,
        courtAppearanceId = courtAppearanceId,
        source = source,
      ),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun delete(
    prisonerId: String,
    periodLengthId: String,
    sentenceId: String,
    courtChargeId: String,
    courtCaseId: String,
    courtAppearanceId: String,
    source: EventSource,
  ) {
    snsService.publishDomainEvent(
      "sentence.period-length.deleted",
      "Sentence period length deleted",
      generateDetailsUri(periodLengthLookupPath, periodLengthId),
      ZonedDateTime.now(),
      HmppsPeriodLengthMessage(
        periodLengthId = periodLengthId,
        sentenceId = sentenceId,
        courtChargeId = courtChargeId,
        courtCaseId = courtCaseId,
        courtAppearanceId = courtAppearanceId,
        source = source,
      ),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
