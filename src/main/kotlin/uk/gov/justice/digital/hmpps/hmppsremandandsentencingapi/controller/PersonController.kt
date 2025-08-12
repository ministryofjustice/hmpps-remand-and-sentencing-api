package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PersonDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ConsecutiveToSentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtCaseService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PersonService
import java.time.LocalDate

@RestController
@RequestMapping("/person", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "person-controller", description = "Get person details")
class PersonController(private val personService: PersonService, private val courtCaseService: CourtCaseService, private val consecutiveToSentenceService: ConsecutiveToSentenceService, private val dpsDomainEventService: DpsDomainEventService) {

  @GetMapping("/{prisonerId}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve person details",
    description = "This endpoint will retrieve person details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns person details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getPersonDetails(@PathVariable prisonerId: String): PersonDetails = personService.getPersonDetailsByPrisonerId(prisonerId)

  @GetMapping("/{prisonerId}/sentenced-court-cases")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Retrieve all sentenced court cases for prisoner",
    description = "This endpoint will retrieve all sentenced court cases for prisoner",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentenced court cases"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getSentencedCourtCases(@PathVariable prisonerId: String): CourtCases = courtCaseService.getSentencedCourtCases(prisonerId).let { (courtCases, eventsToEmit) ->
    dpsDomainEventService.emitEvents(eventsToEmit)
    courtCases
  }

  @GetMapping("/{prisonerId}/has-sentence-to-chain-to")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Determine if there can be a sentence to chain to",
    description = "This endpoint will determine whether there can be a sentence which can be selected for a consecutive to chain",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns whether there is a sentence for a consecutive to chain"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun hasSentenceToChainTo(@PathVariable prisonerId: String, @RequestParam(name = "beforeOrOnAppearanceDate", required = true) beforeOrOnAppearanceDate: LocalDate, @RequestParam(name = "bookingId", required = false) bookingId: String?): HasSentenceToChainToResponse = consecutiveToSentenceService.hasSentenceToChainTo(prisonerId, beforeOrOnAppearanceDate, bookingId)

  @GetMapping("/{prisonerId}/sentences-to-chain-to")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "retrieve all sentences to chain to",
    description = "This endpoint will retrieve all sentences which can be used in a consecutive to chain",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns all sentences for a consecutive to chain"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun sentencesToChainTo(@PathVariable prisonerId: String, @RequestParam(name = "beforeOrOnAppearanceDate", required = true) beforeOrOnAppearanceDate: LocalDate, @RequestParam(name = "bookingId", required = false) bookingId: String?): SentencesToChainToResponse = consecutiveToSentenceService.sentencesToChainTo(prisonerId, beforeOrOnAppearanceDate, bookingId).let { (response, eventsToEmit) ->
    dpsDomainEventService.emitEvents(eventsToEmit)
    response
  }
}
