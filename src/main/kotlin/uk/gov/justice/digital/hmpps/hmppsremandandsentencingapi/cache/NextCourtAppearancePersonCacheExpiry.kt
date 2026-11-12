package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import com.github.benmanes.caffeine.cache.Expiry
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.PersonNextCourtAppearance
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class NextCourtAppearancePersonCacheExpiry(
  private val clock: Clock = Clock.system(ZoneId.of("Europe/London")),
  private val maxResultTtl: Duration = Duration.ofHours(1),
) : Expiry<Any, Any> {

  override fun expireAfterCreate(
    key: Any,
    value: Any,
    currentTime: Long,
  ): Long = calculateExpiryNanos(value)

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

  private fun calculateExpiryNanos(value: Any): Long = when (value) {
    is PersonNextCourtAppearance -> {
      val now = ZonedDateTime.now(clock)
      val appearanceTime = value.dateTime.atZone(clock.zone)
      ChronoUnit.NANOS.between(now, appearanceTime).coerceIn(0L, maxResultTtl.toNanos())
    }
    else -> maxResultTtl.toNanos()
  }
}
