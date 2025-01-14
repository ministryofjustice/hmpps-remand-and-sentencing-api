package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceDomainEventService

@RestController
@RequestMapping("/legacy/sentence", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-sentence-controller", description = "CRUD operations for syncing sentence data from NOMIS Offender sentences into remand and sentencing api database.")
class LegacySentenceController(private val legacySentenceService: LegacySentenceService, private val eventService: SentenceDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a sentence",
    description = "Synchronise a creation of sentence from NOMIS Offender sentences into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "sentence created"),
      ApiResponse(responseCode = "422", description = "Unprocessable entity, charge must not be already sentenced"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_SENTENCE_RW')")
  fun create(@RequestBody sentence: LegacyCreateSentence): LegacySentenceCreatedResponse {
    return legacySentenceService.create(sentence).also {
      eventService.create(it.prisonerId, it.lifetimeUuid.toString(), it.chargeLifetimeUuid.toString(), EventSource.NOMIS)
    }
  }
}
