package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails

/**
 * This client is only available in Dev at this time
 * check back Dec 2026 and remove PrisonRegisterClientNoop
 * once fully instated
 */
@ConditionalOnProperty(
  prefix = "features.client-api.prison-register",
  name = ["enabled"],
  havingValue = "true",
)
@Component
class PrisonRegisterRestClient(@Qualifier("prisonRegisterWebClient") private val webClient: WebClient) : PrisonRegisterClient {

  override fun getAgencyDetails(agencyId: String): AgencyDetails? = webClient
    .get()
    .uri("/api/agencies/{agencyId}", agencyId)
    .retrieve()
    .bodyToMono(typeReference<AgencyDetails>())
    .block()
}
