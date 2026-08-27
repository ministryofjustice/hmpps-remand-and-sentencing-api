package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.admin.FixSingleSentenceMultipleChargesPeople
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.BulkFixManyChargesToSentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@RestController
@RequestMapping("/person-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "person-admin-controller", description = "Person admin")
class PersonAdminController(private val bulkFixManyChargesToSentenceService: BulkFixManyChargesToSentenceService, private val serviceUserService: ServiceUserService) {

  @PostMapping("/fix-many-charges-to-sentence")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Fixes the single sentence to many charges issue for prisoner ids",
    description = "Fix all single sentence to many charges for prisoner ids",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "202", description = "fix triggered"),
    ],
  )
  fun cleanupManyChargesToSentence(@RequestBody fixSingleSentenceMultipleChargesPeople: FixSingleSentenceMultipleChargesPeople) {
    bulkFixManyChargesToSentenceService.fixPeople(fixSingleSentenceMultipleChargesPeople, serviceUserService.getUsername())
  }
}
