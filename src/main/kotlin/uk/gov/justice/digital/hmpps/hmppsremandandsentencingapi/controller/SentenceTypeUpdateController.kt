package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceTypeUpdateService
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "sentence-type-update-controller", description = "Sentence Type Updates")
class SentenceTypeUpdateController(
  private val sentenceTypeUpdateService: SentenceTypeUpdateService,
  private val sentenceDomainEventService: SentenceDomainEventService,
  private val courtCaseRepository: CourtCaseRepository,
  private val sentenceRepository: SentenceRepository,
) {

  @PostMapping("/court-case/{courtCaseUuid}/sentences/update-types")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_SENTENCING__RECORD_RECALL_RW')")
  @Operation(
    summary = "Update sentence types for unknown pre-recall sentences",
    description = "This endpoint updates the sentence types for sentences that are currently marked as 'unknown pre-recall sentence'. All updates are performed atomically - if any update fails, all changes are rolled back.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "All sentence types successfully updated"),
      ApiResponse(responseCode = "400", description = "Invalid request (invalid sentence type, missing fields)"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Court case or sentence not found"),
      ApiResponse(responseCode = "422", description = "Business rule violation (e.g., sentence not in 'unknown pre-recall sentence' state)"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun updateSentenceTypes(
    @PathVariable courtCaseUuid: UUID,
    @Valid @RequestBody request: UpdateSentenceTypeRequest,
  ): UpdateSentenceTypeResponse {
    val response = sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    
    // Emit domain events for each updated sentence
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString())!!
    response.updatedSentenceUuids.forEach { sentenceUuid ->
      val sentence = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid)!!
      val charge = sentence.charge
      val appearance = charge.appearanceCharges.firstOrNull { it.appearance?.statusId == uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus.ACTIVE }?.appearance
      
      if (appearance != null) {
        sentenceDomainEventService.update(
          courtCase.prisonerId,
          sentenceUuid.toString(),
          charge.chargeUuid.toString(),
          courtCaseUuid.toString(),
          appearance.appearanceUuid.toString(),
          EventSource.DPS
        )
      }
    }
    
    return response
  }
}
