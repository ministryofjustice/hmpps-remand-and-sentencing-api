package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteImmigrationDetentionResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveImmigrationDetentionResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ImmigrationDetentionService
import java.util.UUID

@RestController
@RequestMapping("/immigration-detention", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "immigration-detention-controller", description = "Immigration Detention")
class ImmigrationDetentionController(
  private val immigrationDetentionService: ImmigrationDetentionService,
  private val dpsDomainEventService: DpsDomainEventService,
) {

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW')")
  @Operation(
    summary = "Create an immigration detention record",
    description = "This endpoint will create a record for managing immigration detention",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns immigration detention record UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createImmigrationDetention(
    @RequestBody createImmigrationDetention: CreateImmigrationDetention,
  ): SaveImmigrationDetentionResponse = immigrationDetentionService.createImmigrationDetention(createImmigrationDetention).let { (response, eventsToEmit) ->
    dpsDomainEventService.emitEvents(eventsToEmit)
    response
  }

  @GetMapping("/{immigrationDetentionUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW')")
  @Operation(
    summary = "Retrieve an immigration detention record",
    description = "This endpoint will retrieve the details of an immigration detention record",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns immigration detention details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no immigration detention uuid"),
    ],
  )
  fun getImmigrationDetention(
    @PathVariable immigrationDetentionUuid: UUID,
  ): ImmigrationDetention = immigrationDetentionService.findImmigrationDetentionByUuid(immigrationDetentionUuid)

  @GetMapping("/person/{prisonerId}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW')")
  @Operation(
    summary = "Retrieve all active immigration detention records for a person",
    description = "This endpoint will retrieve  all active immigration detention records for a person",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns all active immigration detention records for person"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getImmigrationDetentionByPrisonerId(
    @PathVariable prisonerId: String,
  ): List<ImmigrationDetention> = immigrationDetentionService.findImmigrationDetentionByPrisonerId(prisonerId)

  @GetMapping("/person/{prisonerId}/latest")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW')")
  @Operation(
    summary = "Retrieve the last active record for the prisoner, sorted by created date descending",
    description = "This endpoint will retrieve the last active immigration detention record for the prisoner, sorted by created date descending",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns all active immigration detention records for person"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "No active records found for this person"),
    ],
  )
  fun getLatestImmigrationDetentionByPrisonerId(
    @PathVariable prisonerId: String,
  ): ResponseEntity<Any> = immigrationDetentionService.findLatestImmigrationDetentionByPrisonerId(prisonerId)?.let {
    ResponseEntity.status(
      HttpStatus.OK,
    ).body(it)
  } ?: ResponseEntity.notFound().build()

  @PutMapping("/{immigrationDetentionUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW')")
  @Operation(
    summary = "Update an immigration detention record (or create one with the passed in details)",
    description = "This endpoint will update an immigration detention record (or create one with the passed in details)",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court case UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun updateImmigrationDetention(
    @RequestBody immigrationDetention: CreateImmigrationDetention,
    @PathVariable immigrationDetentionUuid: UUID,
  ): SaveImmigrationDetentionResponse = immigrationDetentionService.updateImmigrationDetention(immigrationDetention, immigrationDetentionUuid)
    .let { (response, eventsToEmit) ->
      dpsDomainEventService.emitEvents(eventsToEmit)
      response
    }

  @DeleteMapping("/{immigrationDetentionUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW')")
  @Operation(
    summary = "Delete an immigration detention record",
    description = "This endpoint will delete an immigration detention record",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Immigration Detention deleted"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Immigration Detention not found"),
    ],
  )
  fun deleteImmigrationDetention(
    @PathVariable immigrationDetentionUuid: UUID,
  ): DeleteImmigrationDetentionResponse = immigrationDetentionService.deleteImmigrationDetention(immigrationDetentionUuid)
    .let { (response, eventsToEmit) ->
      dpsDomainEventService.emitEvents(eventsToEmit)
      response
    }
}
