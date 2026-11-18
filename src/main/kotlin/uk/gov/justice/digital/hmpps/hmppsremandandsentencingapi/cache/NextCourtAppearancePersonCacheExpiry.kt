package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.PersonNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants.Companion.ZONE_ID
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Component
class NextCourtAppearancePersonCacheExpiry(
  override val maxResultTtl: Duration = Duration.ofHours(1),
  override val maximumSize: Long = 100_000L,
  private val clock: Clock = Clock.system(ZoneId.of(ZONE_ID)),
) : CacheExpiry {

  override val cacheName = Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY

  override fun expireAfterCreate(
    key: Any,
    value: Any,
    currentTime: Long,
  ): Long = calculateExpiryNanos(value)

  private fun calculateExpiryNanos(value: Any): Long = when (value) {
    is PersonNextCourtAppearance -> {
      val now = ZonedDateTime.now(clock)
      val appearanceTime = value.dateTime.atZone(clock.zone)
      ChronoUnit.NANOS.between(now, appearanceTime).coerceIn(0L, maxResultTtl.toNanos())
    }
    else -> maxResultTtl.toNanos()
  }
}
