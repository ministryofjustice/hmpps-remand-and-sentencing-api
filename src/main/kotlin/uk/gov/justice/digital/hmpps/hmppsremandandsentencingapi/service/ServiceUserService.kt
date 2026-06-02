package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.security.core.Authentication

interface ServiceUserService {
  fun getCurrentAuthentication(): Authentication
  fun getUsername(): String
}
