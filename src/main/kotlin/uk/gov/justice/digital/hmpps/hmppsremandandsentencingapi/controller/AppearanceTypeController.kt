package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.AppearanceTypeService
import java.util.UUID

@RestController
@RequestMapping("/appearance-type", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "appearance-type-controller", description = "Appearance Type")
class AppearanceTypeController(private val appearanceTypeService: AppearanceTypeService) {

  @GetMapping("/all")
  @Operation(
    summary = "Get all appearance types",
    description = "This endpoint will get all appearance types",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns appearance types"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun getAllAppearanceTypes(): List<AppearanceType> = appearanceTypeService.getAll()

  @GetMapping("/{appearanceTypeUuid}")
  @Operation(
    summary = "Get appearance type by uuid",
    description = "This endpoint will get appearance type by uuid",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns appearance type"),
      ApiResponse(responseCode = "404", description = "Appearance type not found"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun getAppearanceTypeById(@PathVariable appearanceTypeUuid: UUID): AppearanceType = appearanceTypeService.findByUuid(appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type found at $appearanceTypeUuid")
}
