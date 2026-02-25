package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtCaseMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TempController(private val telemetryClient: TelemetryClient, private val objectMapper: ObjectMapper) {

  @PostMapping("/temp/generate-failed-court-case-event-publish-telemetry")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun generateFailedEventPublishTelemetry() {
    telemetryClient.trackEvent("failed-domain-event-published", mapOf("eventType" to "court-case.updated", "description" to "Court case updated", "additionalInformation" to objectMapper.writeValueAsString(HmppsCourtCaseMessage("1")), "personReference" to objectMapper.writeValueAsString(PersonReference(listOf(PersonReferenceType("NOMS", "1234"))))), null)
  }
}
