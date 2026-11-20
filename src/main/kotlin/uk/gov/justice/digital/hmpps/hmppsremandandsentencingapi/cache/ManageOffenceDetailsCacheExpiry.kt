package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants

@Component
class ManageOffenceDetailsCacheExpiry : CacheExpiry {

  override val cacheName = Constants.MANAGE_OFFENCE_DETAILS_CACHE_KEY

  override fun expireAfterCreate(
    key: Any,
    value: Any,
    currentTime: Long,
  ): Long = maxResultTtl.toNanos()
}
