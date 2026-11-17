package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import com.github.benmanes.caffeine.cache.Expiry
import java.time.Duration

interface CacheExpiry : Expiry<Any, Any> {
  val maxResultTtl: Duration
    get() = Duration.ofMinutes(15)
  val maximumSize: Long
    get() = 10_000L
  val cacheName: String

  override fun expireAfterUpdate(
    key: Any,
    value: Any,
    currentTime: Long,
    currentDuration: Long,
  ): Long = currentDuration

  override fun expireAfterRead(
    key: Any,
    value: Any,
    currentTime: Long,
    currentDuration: Long,
  ): Long = currentDuration
}
