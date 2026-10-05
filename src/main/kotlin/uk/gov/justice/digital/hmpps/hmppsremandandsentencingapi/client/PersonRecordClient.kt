package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison

@Component
class PersonRecordClient(@Qualifier("personRecordWebClient") private val webClient: WebClient) {

  fun getPersonPrison(prisonerId: String): PersonPrison = webClient
    .get()
    .uri("/person/prison/{prisonerId}", prisonerId)
    .retrieve()
    .bodyToMono(typeReference<PersonPrison>())
    .block()!!
}
