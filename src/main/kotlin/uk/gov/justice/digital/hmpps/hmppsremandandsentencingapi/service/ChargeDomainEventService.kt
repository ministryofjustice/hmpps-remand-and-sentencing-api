package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtChargeMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import java.time.ZonedDateTime

@Service
class ChargeDomainEventService(
  private val snsService: SnsService,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${court.charge.getByIdPath}") private val courtChargeLookupPath: String,
) {

  fun create(prisonerId: String, chargeId: String, courtCaseId: String, source: String) {
    snsService.publishDomainEvent(
      "charge.inserted",
      "Charge inserted",
      generateDetailsUri(courtChargeLookupPath, chargeId),
      ZonedDateTime.now(),
      HmppsCourtChargeMessage(chargeId, courtCaseId, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  fun update(prisonerId: String, chargeId: String, courtCaseId: String, source: String) {
    snsService.publishDomainEvent(
      "charge.updated",
      "Charge updated",
      generateDetailsUri(courtChargeLookupPath, chargeId),
      ZonedDateTime.now(),
      HmppsCourtChargeMessage(chargeId, courtCaseId, source),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
