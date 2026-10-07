package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails

@Component
class ManageOffencesApiClient(@Qualifier("manageOffencesApiWebClient") private val webClient: WebClient) {

  fun getOffenceDetails(offenceCode: String): OffenceDetails = webClient
    .get()
    .uri("/offences/code/unique/{offenceCode}", offenceCode)
    .retrieve()
    .bodyToMono(typeReference<OffenceDetails>())
    .block()!!
}
