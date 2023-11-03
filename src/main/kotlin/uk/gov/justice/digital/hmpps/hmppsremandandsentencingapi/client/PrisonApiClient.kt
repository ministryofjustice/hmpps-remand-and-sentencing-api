package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonerDetails

@Component
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {

  suspend fun getOffenderDetail(prisonerId: String): PrisonerDetails {
    return webClient
      .get()
      .uri("/api/offenders/{prisonerId}", prisonerId)
      .retrieve()
      .awaitBody<PrisonerDetails>()
  }
}
