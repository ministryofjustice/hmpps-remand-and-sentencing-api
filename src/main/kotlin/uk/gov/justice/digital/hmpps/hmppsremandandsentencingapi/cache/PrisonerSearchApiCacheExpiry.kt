package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants

@Component
class PrisonerSearchApiCacheExpiry : CacheExpiry {

  override val cacheName = Constants.PRISONER_SEARCH_API_CACHE_KEY

  override fun expireAfterCreate(
    key: Any,
    value: Any,
    currentTime: Long,
  ): Long = maxResultTtl.toNanos()
}
