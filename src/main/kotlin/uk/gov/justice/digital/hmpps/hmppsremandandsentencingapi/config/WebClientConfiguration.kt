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
  @param:Value("\${court-register.api.url:}") private val courtRegisterApiUri: String,
  @param:Value("\${document.management.api.url}") private val documentManagementApiUri: String,
  @param:Value("\${adjustments.api.url}") private val adjustmentsApiUri: String,
  @param:Value("\${court-data-ingestion.api.url}") private val courtDataIngestionApiUri: String,
  @param:Value("\${person-record.api.url}") private val personRecordUri: String,
  @param:Value("\${prison-register.api.url}") private val prisonRegisterUri: String,
  @param:Value("\${prisoner-search.api.url}") private val prisonerSearchUri: String,
  @param:Value("\${manage-offences.api.url}") private val manageOffencesUri: String,
  @param:Value("\${hmpps.auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
) {

  @Deprecated("This is a NOMIS Wrapper Service and should be avoided")
  @Bean
  fun prisonApiWebClient(webclientBuilder: WebClient.Builder): WebClient = webclientBuilder
    .baseUrl(prisonApiUri)
    .filter(addAuthHeaderFilterFunction())
    .build()

  @Bean
  fun courtRegisterApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "court-register-api",
    courtRegisterApiUri,
  )

  @Bean
  fun personRecordWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "person-record",
    personRecordUri,
  )

  @Bean
  fun prisonRegisterWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "prison-register",
    prisonRegisterUri,
  )

  @Bean
  fun prisonerSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "prisoner-search",
    prisonerSearchUri,
  )

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val authenticationToken: Jwt = SecurityContextHolder.getContext()
      .authentication!!
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

  @Bean
  fun manageOffencesApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "manage-offences-api",
    manageOffencesUri,
  )

  private fun addDocumentManagementHeadersFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val authentication: Authentication = SecurityContextHolder.getContext()
      .authentication!!
    val filtered = ClientRequest.from(request)
      .header("Username", authentication.name)
      .header("Service-Name", "Remand and Sentencing")
      .build()
    next.exchange(filtered)
  }

  @Bean
  fun adjustmentsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "adjustments-api",
    adjustmentsApiUri,
  )

  @Bean
  fun courtDataIngestionApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    "court-data-ingestion-api",
    courtDataIngestionApiUri,
  )

  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)
}
