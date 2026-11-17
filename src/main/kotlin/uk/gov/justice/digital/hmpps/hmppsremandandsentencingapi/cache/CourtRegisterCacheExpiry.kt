package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants

@Component
class CourtRegisterCacheExpiry : CacheExpiry {

  override val cacheName = Constants.COURT_REGISTER_BY_COURT_CODE_CACHE_KEY

  override fun expireAfterCreate(
    key: Any,
    value: Any,
    currentTime: Long,
  ): Long = maxResultTtl.toNanos()
}
