package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import java.util.UUID

@Component
class CourtRegisterApiClient(@Qualifier("courtRegisterApiWebClient") private val webClient: WebClient) {

  fun getCourtRegister(courtCode: String): CourtRegister? = webClient
    .get()
    .uri("/courts/id/{courtId}", courtCode)
    .retrieve()
    .bodyToMono(typeReference<CourtRegister>())
    .block()

  fun getCourtRegisterByHmctsId(hmctsCourtId: UUID): CourtRegister? = webClient
    .get()
    .uri("/courts/cp/{hmctsCourtId}", hmctsCourtId)
    .retrieve()
    .bodyToMono(typeReference<CourtRegister>())
    .block()
}
