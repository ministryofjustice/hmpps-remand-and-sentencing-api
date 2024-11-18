package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtCaseMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import java.time.ZonedDateTime

@Service
class CourtCaseDomainEventService(
  private val snsService: SnsService,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${court.case.getByIdPath}") private val courtCaseLookupPath: String,
) {

  fun create(id: String, prisonerId: String, source: String) {
    snsService.publishDomainEvent(
      "court-case.inserted",
      "Court case inserted",
      generateDetailsUri(courtCaseLookupPath, id),
      ZonedDateTime.now(),
      HmppsCourtCaseMessage(id, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun update(id: String, prisonerId: String, source: String) {
    snsService.publishDomainEvent(
      "court-case.updated",
      "Court case updated",
      generateDetailsUri(courtCaseLookupPath, id),
      ZonedDateTime.now(),
      HmppsCourtCaseMessage(id, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
