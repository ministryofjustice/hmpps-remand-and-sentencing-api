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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeIsValid
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes.AllSentenceTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes.CreateSentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes.SentenceTypeDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.MigrateSentenceRecordsSentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceTypeService
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/sentence-type", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "sentence-type-controller", description = "Sentence Type")
class SentenceTypeController(private val sentenceTypesService: SentenceTypeService, private val migrateSentenceRecordsSentenceType: MigrateSentenceRecordsSentenceType) {

  @GetMapping("/search")
  @Operation(
    summary = "Search all sentence types",
    description = "This endpoint will search all sentence types",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence types"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun searchSentenceTypes(@RequestParam("age") age: Int, @RequestParam("convictionDate") convictionDate: LocalDate, @RequestParam(name = "statuses", defaultValue = "ACTIVE", required = false) statuses: List<ReferenceEntityStatus>, @RequestParam("offenceDate") offenceDate: LocalDate): List<SentenceType> = sentenceTypesService.search(age, convictionDate, statuses, offenceDate)

  @GetMapping("/{sentenceTypeUuid}")
  @Operation(
    summary = "Get Sentence type by UUID",
    description = "This endpoint will retrieve sentence type by UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at uuid"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getSentenceTypeByUuid(@PathVariable sentenceTypeUuid: UUID): SentenceType = sentenceTypesService.findByUuid(sentenceTypeUuid) ?: throw EntityNotFoundException("No sentence type found at $sentenceTypeUuid")

  @GetMapping("/uuid/multiple")
  @Operation(
    summary = "get all sentence types by uuids",
    description = "This endpoint will get all sentence types by uuids",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence types"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getSentenceTypesByIds(@RequestParam("uuids") uuids: List<UUID>): List<SentenceType> = sentenceTypesService.findByUuids(uuids)

  @GetMapping("/{sentenceTypeUuid}/is-still-valid")
  @Operation(
    summary = "Check the sentence type is still valid",
    description = "This endpoint will determine if the sentence type is still valid with the age, conviction date, statuses, offence date parameters",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns whether the sentence type is still valid"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun sentenceTypeStillValid(@PathVariable sentenceTypeUuid: UUID, @RequestParam("age") age: Int, @RequestParam("convictionDate") convictionDate: LocalDate, @RequestParam(name = "statuses", defaultValue = "ACTIVE", required = false) statuses: List<ReferenceEntityStatus>, @RequestParam("offenceDate") offenceDate: LocalDate): SentenceTypeIsValid = sentenceTypesService.sentenceTypeIsStillValid(sentenceTypeUuid, age, convictionDate, statuses, offenceDate)

  @GetMapping("/all")
  @Operation(
    summary = "Search all sentence types",
    description = "This endpoint will search all sentence types",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence types"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun getAllSentenceTypes(): AllSentenceTypes = sentenceTypesService.getAllSentenceTypes()

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Create sentence type",
    description = "This endpoint will create a new sentence type and migrate any sentence data over that needs to be mapped to the newly created sentence type",
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
  fun createSentenceType(@Valid @RequestBody createSentenceType: CreateSentenceType): SentenceTypeDetails = sentenceTypesService.createSentenceType(createSentenceType).let { createdSentenceTypeEntity ->
    migrateSentenceRecordsSentenceType.migrateSentenceRecordsSentenceType(createdSentenceTypeEntity)
    SentenceTypeDetails.from(createdSentenceTypeEntity)
  }

  @PutMapping("/{sentenceTypeUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Update sentence type",
    description = "This endpoint will update an existing sentence type and migrate any sentence data over that needs to be mapped to the newly updated sentence type",
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
  fun updateSentenceType(@PathVariable sentenceTypeUuid: UUID, @Valid @RequestBody updateSentenceType: CreateSentenceType): SentenceTypeDetails = sentenceTypesService.updateSentenceType(sentenceTypeUuid, updateSentenceType).let { (entity, migrateSentenceData) ->
    if (migrateSentenceData) {
      migrateSentenceRecordsSentenceType.migrateSentenceRecordsSentenceType(entity)
    }
    SentenceTypeDetails.from(entity)
  }

  @GetMapping("/{sentenceTypeUuid}/details")
  @Operation(
    summary = "Get Sentence type details by UUID",
    description = "This endpoint will retrieve sentence type details by UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence type details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at uuid"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getSentenceTypeDetailsByUuid(@PathVariable sentenceTypeUuid: UUID): SentenceTypeDetails = sentenceTypesService.findDetailsByUuid(sentenceTypeUuid) ?: throw EntityNotFoundException("No sentence type found at $sentenceTypeUuid")
}
