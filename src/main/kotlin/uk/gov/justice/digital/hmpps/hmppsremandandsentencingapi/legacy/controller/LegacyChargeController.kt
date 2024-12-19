package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyChargeCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyChargeService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ChargeDomainEventService
import java.util.UUID

@RestController
@RequestMapping("/legacy/charge", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-charge-controller", description = "CRUD operations for syncing charge data from NOMIS Offender charges into remand and sentencing api database.")
class LegacyChargeController(private val legacyChargeService: LegacyChargeService, private val eventService: ChargeDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a charge",
    description = "Synchronise a creation of charge from NOMIS Offender charges into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "charge created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW')")
  fun create(@RequestBody charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    return legacyChargeService.create(charge).also {
      eventService.create(it.prisonerId, it.lifetimeUuid.toString(), it.courtCaseUuid, EventSource.NOMIS)
    }
  }

  @PutMapping("/{lifetimeUuid}")
  @Operation(
    summary = "Update a charge in all appearances",
    description = "Synchronise an update of charge in all appearances from NOMIS Offender charges into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "charge updated"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW')")
  fun update(@PathVariable lifetimeUuid: UUID, @RequestBody charge: LegacyUpdateWholeCharge) {
    legacyChargeService.updateInAllAppearances(lifetimeUuid, charge)
  }

  @PutMapping("/{lifetimeUuid}/appearance/{appearanceLifetimeUuid}")
  @Operation(
    summary = "Update a charge in an appearance",
    description = "Synchronise an update of charge within an appearance from NOMIS Offender charges into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "charge updated"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW')")
  fun updateInAppearance(@PathVariable lifetimeUuid: UUID, @PathVariable appearanceLifetimeUuid: UUID, @RequestBody charge: LegacyUpdateCharge) {
    legacyChargeService.updateInAppearance(lifetimeUuid, appearanceLifetimeUuid, charge).also { (entityChangeStatus, legacyChargeCreatedResponse) ->
      if (entityChangeStatus == EntityChangeStatus.EDITED) {
        eventService.update(legacyChargeCreatedResponse.prisonerId, legacyChargeCreatedResponse.lifetimeUuid.toString(), appearanceLifetimeUuid.toString(), legacyChargeCreatedResponse.courtCaseUuid, EventSource.NOMIS)
      }
    }
  }

  @GetMapping("/{lifetimeUuid}")
  @Operation(
    summary = "retrieve a charge",
    description = "This endpoint will retrieve charge details.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns charge details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW', 'ROLE_REMAND_AND_SENTENCING_CHARGE_RO')")
  fun get(@PathVariable lifetimeUuid: UUID): LegacyCharge {
    return legacyChargeService.get(lifetimeUuid)
  }

  @DeleteMapping("/{lifetimeUuid}")
  @Operation(
    summary = "Delete Charge",
    description = "Synchronise a deletion of charge from NOMIS offender charges into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW')")
  fun delete(@PathVariable lifetimeUuid: UUID) {
    legacyChargeService.get(lifetimeUuid).also { legacyCharge ->
      legacyChargeService.delete(lifetimeUuid)
      eventService.delete(legacyCharge.prisonerId, legacyCharge.lifetimeUuid.toString(), legacyCharge.courtCaseUuid, EventSource.NOMIS)
    }
  }
}
