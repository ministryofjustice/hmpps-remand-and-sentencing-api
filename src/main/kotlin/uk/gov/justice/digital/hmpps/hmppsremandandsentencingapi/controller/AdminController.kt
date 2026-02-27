package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.admin.RepublishEvents
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService

@RestController
@RequestMapping("/event-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "event-admin-controller", description = "Event Admin tasks")
class AdminController(private val dpsDomainEventService: DpsDomainEventService) {

  @PostMapping("/republish")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun republishEvents(@RequestBody republishEvents: RepublishEvents) {
    dpsDomainEventService.emitEvents(republishEvents.eventsMetadata.toSet())
  }
}
