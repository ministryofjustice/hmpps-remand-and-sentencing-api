package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceAfterOnOtherCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceConsecutiveToDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesAfterOnOtherCourtAppearanceDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceService
import java.util.UUID

@RestController
@Tag(name = "sentence-controller", description = "Sentences")
class SentenceController(private val sentenceService: SentenceService, private val dpsDomainEventService: DpsDomainEventService) {

  @GetMapping("\${court.sentence.getByIdPath}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve sentence details",
    description = "This endpoint will retrieve sentence details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence at uuid"),
    ],
  )
  fun getSentenceDetails(@PathVariable sentenceUuid: UUID): Sentence = sentenceService.findSentenceByUuid(sentenceUuid) ?: throw EntityNotFoundException("No sentence found at $sentenceUuid")

  @GetMapping("/sentence/consecutive-to-details")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Retrieve sentence consecutive to details",
    description = "This endpoint will retrieve consecutive to sentence details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns consecutive to sentence details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getConsecutiveToSentenceDetails(@RequestParam("sentenceUuids", required = true) sentenceUuids: List<UUID>): SentenceConsecutiveToDetailsResponse = sentenceService.findConsecutiveToSentenceDetails(sentenceUuids).let { (response, eventsToEmit) ->
    dpsDomainEventService.emitEvents(eventsToEmit)
    response
  }

  @GetMapping("/sentence/{sentenceUuid}/has-sentences-after-on-other-court-appearance")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Any sentences after on another appearance",
    description = "This endpoint will return true if any other sentence has a consecutive to link to the sentence at sentenceUuid that is on a different court appearance",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns true or false"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun hasSentencesAfterOnOtherCase(@PathVariable sentenceUuid: UUID): HasSentenceAfterOnOtherCourtAppearanceResponse = sentenceService.hasSentencesAfterOnOtherCourtAppearance(sentenceUuid)

  @GetMapping("/sentence/{sentenceUuid}/sentences-after-on-other-court-appearance-details")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Sentences after on other court appearance details",
    description = "This endpoint will return details of the court appearances of sentences after this sentence",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns true or false"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun sentencesAfterOnOtherCourtAppearanceDetails(@PathVariable sentenceUuid: UUID): SentencesAfterOnOtherCourtAppearanceDetailsResponse = sentenceService.sentencesAfterOnOtherCourtAppearanceDetails(sentenceUuid)

  @PostMapping("/sentences/consecutive-chain/check")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Check whether a target sentence is already in a consecutive chain from a source sentence",
    description = "Returns true if the target sentence already appears in any consecutive chain",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200",description = "Returns true or false"),
      ApiResponse(responseCode = "400", description = "Invalid input"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  fun checkConsecutiveChain(@RequestBody request: ConsecutiveChainCheckRequest): Boolean = sentenceService.isTargetAlreadyInConsecutiveChain(
    prisonerId = request.prisonerId,
    appearanceUUID = request.appearanceUuid,
    sourceSentenceUUID = request.sourceSentenceUuid,
    targetSentenceUUID = request.targetSentenceUuid,
    sentencesOnAppearanceFromUI = request.sentences,
  )
}
