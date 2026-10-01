package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonDetails

@Component
class PrisonRegisterClient(@Qualifier("personRecordWebClient") private val webClient: WebClient) {

  fun getPrisonDetails(prisonerId: String): PrisonDetails = webClient
    .get()
    .uri("/prisons/id/{prisonerId}", prisonerId)
    .retrieve()
    .bodyToMono(typeReference<PrisonDetails>())
    .block()!!
}