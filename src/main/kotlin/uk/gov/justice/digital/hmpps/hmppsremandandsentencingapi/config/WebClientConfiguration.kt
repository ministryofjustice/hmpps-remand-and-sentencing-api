package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${prison.api.url}") private val prisonApiUri: String,
  @Value("\${document.management.api.url}") private val documentManagementApiUri: String,
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
  @RequestScope
  fun documentManagementApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManagerUserEnhanced(clientRegistrationRepository), builder.filter(addDocumentManagementHeadersFilterFunction()), documentManagementApiUri, "document-management-api")

  private fun addDocumentManagementHeadersFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val authentication: Authentication = SecurityContextHolder.getContext()
      .authentication
    val filtered = ClientRequest.from(request)
      .header("Username", authentication.name)
      .header("Service-Name", "Remand and Sentencing")
      .build()
    next.exchange(filtered)
  }

  private fun authorizedClientManagerUserEnhanced(clients: ClientRegistrationRepository?): OAuth2AuthorizedClientManager {
    val service: OAuth2AuthorizedClientService = InMemoryOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)

    val defaultClientCredentialsTokenResponseClient = DefaultClientCredentialsTokenResponseClient()

    val authentication = SecurityContextHolder.getContext().authentication

    defaultClientCredentialsTokenResponseClient.setRequestEntityConverter { grantRequest: OAuth2ClientCredentialsGrantRequest ->
      val converter = CustomOAuth2ClientCredentialsGrantRequestEntityConverter()
      val username = authentication.name
      converter.enhanceWithUsername(grantRequest, username)
    }

    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials { clientCredentialsGrantBuilder: OAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder ->
        clientCredentialsGrantBuilder.accessTokenResponseClient(defaultClientCredentialsTokenResponseClient)
      }
      .build()

    manager.setAuthorizedClientProvider(authorizedClientProvider)
    return manager
  }

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    registrationId: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(registrationId)
    return builder.baseUrl(rootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }
}
