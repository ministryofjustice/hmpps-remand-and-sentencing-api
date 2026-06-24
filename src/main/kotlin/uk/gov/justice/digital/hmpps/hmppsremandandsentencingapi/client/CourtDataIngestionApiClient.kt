package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import java.util.UUID

@Component
class CourtDataIngestionApiClient(@Qualifier("courtDataIngestionApiWebClient") private val webClient: WebClient) {

  fun getCourtHearing(courtHearingId: UUID): HmctsCourHearing = webClient
    .get()
    .uri("/court-hearings/$courtHearingId")
    .retrieve()
    .bodyToMono(HmctsCourHearing::class.java)
    .block()!!
}
