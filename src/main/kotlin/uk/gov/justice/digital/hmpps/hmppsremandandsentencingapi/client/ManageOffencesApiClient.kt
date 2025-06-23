package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.Offence

@Component
class ManageOffencesApiClient(@Qualifier("manageOffencesApiWebClient") private val webClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  fun getOffencesByCode(offenceCodes: Set<String>): Map<String, String> {
    if (offenceCodes.isEmpty()) {
      return emptyMap()
    }

    return try {
      val codesParam = offenceCodes.joinToString(",")
      log.debug("Fetching offence descriptions for codes: {}", codesParam)

      val offences: List<Offence> = webClient
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/offences/code/multiple")
            .queryParam("offenceCodes", codesParam)
            .build()
        }
        .retrieve()
        .bodyToMono(typeReference<List<Offence>>())
        .block() ?: emptyList()

      log.debug("Retrieved {} offence descriptions", offences.size)

      offences.associate { offence ->
        offence.code to (offence.description ?: "Description not available")
      }
    } catch (e: WebClientResponseException) {
      log.error(
        "Failed to fetch offence descriptions for codes: {}, status: {}, message: {}",
        offenceCodes,
        e.statusCode,
        e.responseBodyAsString,
        e,
      )
      emptyMap()
    } catch (e: Exception) {
      log.error("Unexpected error fetching offence descriptions for codes: {}", offenceCodes, e)
      emptyMap()
    }
  }
}
