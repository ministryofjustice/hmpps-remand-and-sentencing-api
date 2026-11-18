package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants

@Component
class PersonPrisonCacheExpiry : CacheExpiry {

  override val cacheName = Constants.PERSON_PRISON_CACHE_KEY

  override fun expireAfterCreate(
    key: Any,
    value: Any,
    currentTime: Long,
  ): Long = maxResultTtl.toNanos()
}
