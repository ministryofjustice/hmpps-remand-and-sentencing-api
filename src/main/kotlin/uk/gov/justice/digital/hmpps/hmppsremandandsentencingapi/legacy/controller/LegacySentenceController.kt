package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceDomainEventService
import java.util.UUID

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
  fun create(@RequestBody sentence: LegacyCreateSentence): LegacySentenceCreatedResponse = legacySentenceService.create(sentence).let { responses ->
    responses.forEach {
      eventService.create(it.prisonerId, it.lifetimeUuid.toString(), it.chargeLifetimeUuid.toString(), it.courtCaseId, it.appearanceUuid.toString(), EventSource.NOMIS)
    }
    responses.first()
  }

  @PutMapping("/{lifetimeUuid}")
  @Operation(
    summary = "Update a sentence",
    description = "Synchronise an update of sentence from NOMIS Offender sentences into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "No content"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_SENTENCE_RW')")
  fun update(@PathVariable lifetimeUuid: UUID, @RequestBody sentence: LegacyCreateSentence): ResponseEntity<Void> {
    legacySentenceService.update(lifetimeUuid, sentence).also { (entityChangeStatus, legacySentenceCreatedResponse) ->
      if (entityChangeStatus == EntityChangeStatus.EDITED) {
        eventService.update(legacySentenceCreatedResponse.prisonerId, legacySentenceCreatedResponse.lifetimeUuid.toString(), legacySentenceCreatedResponse.chargeLifetimeUuid.toString(), legacySentenceCreatedResponse.courtCaseId, legacySentenceCreatedResponse.appearanceUuid.toString(), EventSource.NOMIS)
      }
    }
    return ResponseEntity.noContent().build()
  }

  @GetMapping("/{lifetimeUuid}")
  @Operation(
    summary = "retrieve a sentence",
    description = "This endpoint will retrieve sentence details.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_SENTENCE_RW', 'ROLE_REMAND_AND_SENTENCING_SENTENCE_RO')")
  fun get(@PathVariable lifetimeUuid: UUID): LegacySentence = legacySentenceService.get(lifetimeUuid)

  @DeleteMapping("/{lifetimeUuid}")
  @Operation(
    summary = "Delete Sentence",
    description = "Synchronise a deletion of sentence from NOMIS offender charges into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_SENTENCE_RW')")
  fun delete(@PathVariable lifetimeUuid: UUID) {
    legacySentenceService.get(lifetimeUuid).also { legacySentence ->
      legacySentenceService.delete(lifetimeUuid)
      eventService.delete(legacySentence.prisonerId, legacySentence.lifetimeUuid.toString(), legacySentence.chargeLifetimeUuid.toString(), legacySentence.courtCaseId, legacySentence.appearanceUuid.toString(), EventSource.NOMIS)
    }
  }
}
