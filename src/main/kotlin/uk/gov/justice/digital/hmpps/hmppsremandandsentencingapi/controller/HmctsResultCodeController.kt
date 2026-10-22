package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FieldErrorErrorResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode.CreateHmctsResultCode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode.HmctsResultCode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsResultCodeService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.datafix.MigrateChargeRecordOutcomes
import java.util.UUID

@RestController
@RequestMapping("/hmcts-result-code", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "hmcts-result-code-controller", description = "Hmcts Result Codes")
class HmctsResultCodeController(private val hmctsResultCodeService: HmctsResultCodeService, private val migrateChargeRecordOutcomes: MigrateChargeRecordOutcomes) {

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Create hmcts result code",
    description = "This endpoint will create a new hmcts result code that will be used to map court data to charge outcome",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "400", description = "Bad request", content = [Content(mediaType = "application/json", schema = Schema(implementation = FieldErrorErrorResponse::class))]),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createHmctsResultCode(@Valid @RequestBody createHmctsResultCode: CreateHmctsResultCode): HmctsResultCode = hmctsResultCodeService.createHmctsResultCode(createHmctsResultCode)

  @PutMapping("/{hmctsResultCodeUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Update hmcts result code",
    description = "This endpoint will update hmcts result code that will be used to map court data to charge outcome",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "400", description = "Bad request", content = [Content(mediaType = "application/json", schema = Schema(implementation = FieldErrorErrorResponse::class))]),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun updateHmctsResultCode(@PathVariable hmctsResultCodeUuid: UUID, @Valid @RequestBody updateHmctsResultCode: CreateHmctsResultCode): HmctsResultCode = hmctsResultCodeService.updateHmctsResultCode(hmctsResultCodeUuid, updateHmctsResultCode)

  @GetMapping("/status")
  @Operation(
    summary = "Get all hmcts result codes by statuses",
    description = "This endpoint will get all hmcts result codes by statuses",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns hmcts result codes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getAllHmctsResultCodes(@RequestParam("statuses") statuses: List<ReferenceEntityStatus>): List<HmctsResultCode> = hmctsResultCodeService.getAllByStatus(statuses)

  @GetMapping("/{outcomeUuid}")
  @Operation(
    summary = "Get hmcts result code by UUID",
    description = "This endpoint will retrieve hmcts result code by UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns hmcts result code"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no hmcts result code at uuid"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getHmctsResultCodeByUuid(@PathVariable outcomeUuid: UUID): HmctsResultCode = hmctsResultCodeService.findByUuid(outcomeUuid) ?: throw EntityNotFoundException("No hmcts result code found at $outcomeUuid")

  @GetMapping("/uuid/multiple")
  @Operation(
    summary = "get all hmcts result codes by uuids",
    description = "This endpoint will get all hmcts result codes by uuids",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns hmcts result codes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getHmctsResultCodesByIds(@RequestParam("uuids") uuids: List<UUID>): List<HmctsResultCode> = hmctsResultCodeService.findByUuids(uuids)
}
