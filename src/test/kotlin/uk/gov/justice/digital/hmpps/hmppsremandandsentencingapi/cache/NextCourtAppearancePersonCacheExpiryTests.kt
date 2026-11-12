package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.caffeine.CaffeineCache
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.PersonNextCourtAppearance
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class NextCourtAppearancePersonCacheExpiryTests {

  private val londonZone = ZoneId.of("Europe/London")
  private var currentInstant: Instant = ZonedDateTime.of(2026, 5, 10, 10, 0, 0, 0, londonZone).toInstant()

  // A clock and Caffeine ticker synced to the same controllable instant
  private val testClock = object : Clock() {
    override fun getZone(): ZoneId = londonZone
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = currentInstant
  }

  private val testTicker = Ticker {
    Duration.between(Instant.EPOCH, currentInstant).toNanos()
  }

  private val nativeCache = Caffeine.newBuilder()
    .expireAfter(NextCourtAppearancePersonCacheExpiry(testClock))
    .ticker(testTicker)
    .build<Any, Any>()

  // Wrapping in Spring's CaffeineCache mirrors production and handles null -> NullValue.INSTANCE
  private val cache = CaffeineCache("nextCourtAppearancePerson", nativeCache, true)

  @Test
  fun `expires entry once court appearance dateTime has passed when within 1 hour`() {
    val appearanceTime = LocalDateTime.of(2026, 5, 10, 10, 30) // 30 minutes in the future
    val appearance = createAppearance(appearanceTime)

    cache.put("A1234AA", appearance)

    // Advance 29 minutes (still valid)
    advanceTimeBy(Duration.ofMinutes(29))
    assertThat(cache.get("A1234AA", PersonNextCourtAppearance::class.java)).isEqualTo(appearance)

    // Advance 1 more minute to hit 10:30 (expired)
    advanceTimeBy(Duration.ofMinutes(1))
    assertThat(cache.get("A1234AA")).isNull()
  }

  @Test
  fun `caps expiry at 1 hour when court appearance dateTime is more than 1 hour away`() {
    val appearanceTime = LocalDateTime.of(2026, 5, 10, 14, 0) // 4 hours in the future
    val appearance = createAppearance(appearanceTime)

    cache.put("A1234AA", appearance)

    // Advance 59 minutes (still valid)
    advanceTimeBy(Duration.ofMinutes(59))
    assertThat(cache.get("A1234AA", PersonNextCourtAppearance::class.java)).isEqualTo(appearance)

    // Advance 1 more minute to hit 1 hour max TTL (expired even though court date is 3 hours away)
    advanceTimeBy(Duration.ofMinutes(1))
    assertThat(cache.get("A1234AA")).isNull()
  }

  @Test
  fun `expires immediately if court appearance dateTime is already in the past`() {
    val pastAppearance = createAppearance(LocalDateTime.of(2026, 5, 10, 9, 59))

    cache.put("A1234AA", pastAppearance)

    assertThat(cache.get("A1234AA")).isNull()
  }

  @Test
  fun `caches null result and expires after 1 hour`() {
    cache.put("A1234AA", null)

    // Advance 59 minutes -> cache hit returning a ValueWrapper whose value is null
    advanceTimeBy(Duration.ofMinutes(59))
    val cachedWrapper = cache.get("A1234AA")
    assertThat(cachedWrapper).isNotNull
    assertThat(cachedWrapper?.get()).isNull()

    // Advance 1 more minute (total 1 hour elapsed) -> expired (cache miss returns null ValueWrapper)
    advanceTimeBy(Duration.ofMinutes(1))
    assertThat(cache.get("A1234AA")).isNull()
  }

  @Test
  fun `handles daylight saving time transitions accurately within the 1 hour window`() {
    // Set clock to 00:45 GMT on the day clocks go forward (last Sunday of March 2026 is March 29)
    currentInstant = ZonedDateTime.of(2026, 3, 29, 0, 45, 0, 0, londonZone).toInstant()

    // Court appearance at 02:15 BST (only 30 real minutes after 00:45 GMT because 01:00 jumps to 02:00)
    val dstAppearance = createAppearance(LocalDateTime.of(2026, 3, 29, 2, 15))
    cache.put("A1234AA", dstAppearance)

    // Advance 29 real minutes -> still valid
    advanceTimeBy(Duration.ofMinutes(29))
    assertThat(cache.get("A1234AA", PersonNextCourtAppearance::class.java)).isEqualTo(dstAppearance)

    // Advance 1 more real minute (total 30 real minutes elapsed) -> expired
    advanceTimeBy(Duration.ofMinutes(1))
    assertThat(cache.get("A1234AA")).isNull()
  }

  private fun advanceTimeBy(duration: Duration) {
    currentInstant = currentInstant.plus(duration)
  }

  private fun createAppearance(dateTime: LocalDateTime) = PersonNextCourtAppearance(
    caseReference = "CASE123",
    courtCode = "COURT",
    appearanceType = AppearanceType(UUID.randomUUID(), "Appearance Type", 1, false),
    courtAppearanceSubType = null,
    dateTime = dateTime,
  )
}
