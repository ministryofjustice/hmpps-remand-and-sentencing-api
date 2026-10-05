package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails

@Component
class PrisonRegisterClient(@Qualifier("prisonRegisterWebClient") private val webClient: WebClient) {

  fun getAgencyDetails(agencyId: String): AgencyDetails = webClient
    .get()
    .uri("/api/agencies/{agencyId}", agencyId)
    .retrieve()
    .bodyToMono(typeReference<AgencyDetails>())
    .block()!!
}
