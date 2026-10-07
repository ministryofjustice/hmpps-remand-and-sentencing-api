package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonerDetails

@Deprecated("This is a NOMIS Wrapper Service and should be avoided")
@Component
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {

  fun getOffenderDetail(prisonerId: String): PrisonerDetails = webClient
    .get()
    .uri("/api/offenders/{prisonerId}", prisonerId)
    .retrieve()
    .bodyToMono(typeReference<PrisonerDetails>())
    .block()!!
}
