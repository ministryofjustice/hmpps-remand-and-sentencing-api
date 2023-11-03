package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${prison.api.url}") private val prisonApiUri: String,
) {

  @Bean
  fun prisonApiWebClient(webclientBuilder: WebClient.Builder): WebClient {
    return webclientBuilder
      .baseUrl(prisonApiUri)
      .filter(AuthTokenFilterFunction())
      .build()
  }
}
