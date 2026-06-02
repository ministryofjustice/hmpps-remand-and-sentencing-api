package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
@Primary
@Profile("batch")
class BatchServiceUserService : ServiceUserService {

  override fun getCurrentAuthentication(): Authentication = UsernamePasswordAuthenticationToken("BATCH_JOB", null, emptyList())

  override fun getUsername(): String = getCurrentAuthentication().principal.toString()
}
