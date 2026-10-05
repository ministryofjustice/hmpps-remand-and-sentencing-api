package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails

@Component
class PrisonerSearchClient(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {

  fun getPrisoner(prisonerId: String): PrisonSearchDetails = webClient
    .get()
    .uri("/prisoner/{prisonerId}", prisonerId)
    .retrieve()
    .bodyToMono(typeReference<PrisonSearchDetails>())
    .block()!!
}
