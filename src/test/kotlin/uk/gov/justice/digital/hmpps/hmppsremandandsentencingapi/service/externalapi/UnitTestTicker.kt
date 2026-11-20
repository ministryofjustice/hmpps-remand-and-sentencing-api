package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import com.github.benmanes.caffeine.cache.Ticker
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class UnitTestTicker : Ticker {
  private val nanos = AtomicLong()

  override fun read(): Long = nanos.get()

  fun add(duration: Duration) {
    nanos.addAndGet(duration.toNanos())
  }
}
