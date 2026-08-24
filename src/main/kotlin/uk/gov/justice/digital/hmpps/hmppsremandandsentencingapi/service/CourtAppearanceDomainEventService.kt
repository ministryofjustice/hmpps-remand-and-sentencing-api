package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsBreachMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtAppearanceMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import java.time.ZonedDateTime

@Service
class CourtAppearanceDomainEventService(
  private val snsService: SnsService,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${court.appearance.getByIdPath}") private val courtAppearanceLookupPath: String,
) {

  fun create(prisonerId: String, courtAppearanceId: String, courtCaseId: String, source: EventSource, isOnFutureCourtAppearance: Boolean, isBreach: Boolean) {
    snsService.publishDomainEvent(
      "court-appearance.inserted",
      "Court appearance inserted",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      ZonedDateTime.now(),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId, source, isOnFutureCourtAppearance, isBreach),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun update(prisonerId: String, courtAppearanceId: String, courtCaseId: String, source: EventSource, isOnFutureCourtAppearance: Boolean, isBreach: Boolean) {
    snsService.publishDomainEvent(
      "court-appearance.updated",
      "Court appearance updated",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      ZonedDateTime.now(),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId, source, isOnFutureCourtAppearance, isBreach),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun delete(prisonerId: String, courtAppearanceId: String, courtCaseId: String, source: EventSource, isOnFutureCourtAppearance: Boolean, isBreach: Boolean) {
    snsService.publishDomainEvent(
      "court-appearance.deleted",
      "Court appearance deleted",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      ZonedDateTime.now(),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId, source, isOnFutureCourtAppearance, isBreach),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun createBreach(prisonerId: String, courtCaseId: String, courtAppearanceId: String, courtAppearanceIds: Set<String>, chargeIds: Set<String>, sentenceIds: List<String>, periodLengthIds: Set<String>, source: EventSource) {
    snsService.publishDomainEvent(
      "breach.inserted",
      "Breach inserted",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      ZonedDateTime.now(),
      HmppsBreachMessage(courtCaseId, courtAppearanceIds, chargeIds, sentenceIds, periodLengthIds, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
