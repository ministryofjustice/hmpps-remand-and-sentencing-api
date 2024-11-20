package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
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

  fun create(prisonerId: String, courtAppearanceId: String, courtCaseId: String, source: String) {
    snsService.publishDomainEvent(
      "court-appearance.inserted",
      "Court appearance inserted",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      ZonedDateTime.now(),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
