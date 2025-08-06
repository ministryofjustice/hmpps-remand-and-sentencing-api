package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${prison.api.url}") private val prisonApiUri: String,
  @param:Value("\${document.management.api.url}") private val documentManagementApiUri: String,
  @param:Value("\${hmpps.auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
) {

  @Bean
  fun prisonApiWebClient(webclientBuilder: WebClient.Builder): WebClient = webclientBuilder
    .baseUrl(prisonApiUri)
    .filter(addAuthHeaderFilterFunction())
    .build()

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val authenticationToken: Jwt = SecurityContextHolder.getContext()
      .authentication
      .credentials as Jwt
    val tokenString: String = authenticationToken.tokenValue
    val filtered = ClientRequest.from(request)
      .header(HttpHeaders.AUTHORIZATION, "Bearer $tokenString")
      .build()
    next.exchange(filtered)
  }

  @Bean
  fun documentManagementApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.filter(addDocumentManagementHeadersFilterFunction()).authorisedWebClient(
    authorizedClientManager,
    "document-management-api",
    documentManagementApiUri,
  )
  private fun addDocumentManagementHeadersFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val authentication: Authentication = SecurityContextHolder.getContext()
      .authentication
    val filtered = ClientRequest.from(request)
      .header("Username", authentication.name)
      .header("Service-Name", "Remand and Sentencing")
      .build()
    next.exchange(filtered)
  }

  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)
}
