package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.linklatestappearance.LinkLatestCourtAppearanceService

@RestController
@RequestMapping("/next-court-appearance-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
class NextCourtAppearanceAdminController(private val linkLatestCourtAppearanceService: LinkLatestCourtAppearanceService) {

  @PostMapping("/link-latest-court-appearances")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun linkLatestCourtAppearances() {
    linkLatestCourtAppearanceService.linkLatestCourtAppearances()
  }
}
