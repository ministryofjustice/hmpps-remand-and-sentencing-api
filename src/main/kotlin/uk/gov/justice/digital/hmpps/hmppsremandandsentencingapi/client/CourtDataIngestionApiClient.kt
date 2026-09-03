package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import java.util.UUID

@Component
class CourtDataIngestionApiClient(@Qualifier("courtDataIngestionApiWebClient") private val webClient: WebClient) {

  fun getCourtHearing(courtHearingId: UUID, prisonerNumber: String): HmctsCourtHearing = webClient
    .get()
    .uri("/court-hearings/prisoner/$prisonerNumber/hearing/$courtHearingId")
    .retrieve()
    .bodyToMono(HmctsCourtHearing::class.java)
    .block()!!

  fun getHearings(prisonerId: String): List<HmctsCourtHearing> = webClient
    .get()
    .uri("/court-hearings/prisoner/$prisonerId")
    .retrieve()
    .bodyToMono(typeReference<List<HmctsCourtHearing>>())
    .block()!!
}
