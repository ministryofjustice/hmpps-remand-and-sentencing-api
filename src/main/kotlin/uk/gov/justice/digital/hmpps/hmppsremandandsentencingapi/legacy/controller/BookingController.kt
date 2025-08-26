package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.BookingService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyDomainEventService

@RestController
@RequestMapping("/legacy/court-case/booking", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "booking-controller", description = "Create operation for bookings in NOMIS. This is used when court cases are cloned onto different bookings")
class BookingController(private val bookingService: BookingService, private val legacyDomainEventService: LegacyDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates court cases for specific booking",
    description = "Creates all entities for specific bookings. This is when something is created on an old booking and the record is copied over to the latest booking so duplicate records need creating.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "court case created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  fun create(
    @RequestBody bookingCreateCourtCases: BookingCreateCourtCases,
  ): BookingCreateCourtCasesResponse = bookingService.create(bookingCreateCourtCases).let {
    legacyDomainEventService.emitEvents(it.eventsToEmit)
    it.record
  }
}
