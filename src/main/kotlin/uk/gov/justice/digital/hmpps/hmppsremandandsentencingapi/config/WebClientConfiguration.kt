package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${prison.api.url}") private val prisonApiUri: String,
) {

  @Bean
  fun prisonApiWebClient(webclientBuilder: WebClient.Builder): WebClient {
    return webclientBuilder
      .baseUrl(prisonApiUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val authenticationToken: Jwt = SecurityContextHolder.getContext()
        .authentication
        .credentials as Jwt
      val tokenString: String = authenticationToken.tokenValue
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, "Bearer $tokenString")
        .build()
      next.exchange(filtered)
    }
  }
}
