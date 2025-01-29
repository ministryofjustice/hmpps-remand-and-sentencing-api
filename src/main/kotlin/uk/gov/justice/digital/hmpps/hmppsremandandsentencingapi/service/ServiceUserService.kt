package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.AuthAwareAuthenticationToken

@Service
class ServiceUserService {

  fun getCurrentAuthentication(): AuthAwareAuthenticationToken = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
    ?: throw IllegalStateException("User is not authenticated")

  fun getUsername(): String = getCurrentAuthentication().principal
}
