package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.AuthAwareAuthenticationToken

@Service
class ServiceUserService {

  suspend fun getCurrentAuthentication(): AuthAwareAuthenticationToken = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication as AuthAwareAuthenticationToken?
    ?: throw IllegalStateException("User is not authenticated")

  suspend fun getUsername(): String {
    return getCurrentAuthentication().principal
  }
}
