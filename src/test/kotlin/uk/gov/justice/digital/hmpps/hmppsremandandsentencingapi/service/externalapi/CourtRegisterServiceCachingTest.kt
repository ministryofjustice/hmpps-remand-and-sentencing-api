package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import java.time.Duration

@SpringJUnitConfig(CachingTestConfig::class, CourtRegisterService::class)
class CourtRegisterServiceCachingTest {

  @Autowired lateinit var cacheManager: CacheManager

  @Autowired lateinit var ticker: UnitTestTicker

  @Autowired lateinit var client: CourtRegisterApiClient

  @Autowired lateinit var service: CourtRegisterService

  @BeforeEach
  fun reset() {
    clearAllMocks()
    cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
  }

  @Test
  fun `should ensure lookups are cached per court code`() {
    every { client.getCourtRegister(any()) } answers { CourtRegister(firstArg(), "Court", "Description") }

    service.getCourtRegisterByCourtCodeCached("COURT1")
    service.getCourtRegisterByCourtCodeCached("COURT1")
    service.getCourtRegisterByCourtCodeCached("COURT2")

    verify(exactly = 1) { client.getCourtRegister("COURT1") }
    verify(exactly = 1) { client.getCourtRegister("COURT2") }
  }

  @Test
  fun `should ensure entries are evicted once the ttl has passed`() {
    every { client.getCourtRegister("COURT1") } returns CourtRegister("COURT1", "Court", "Description")

    service.getCourtRegisterByCourtCodeCached("COURT1")
    ticker.add(Duration.ofMinutes(14))
    service.getCourtRegisterByCourtCodeCached("COURT1")
    verify(exactly = 1) { client.getCourtRegister("COURT1") }

    ticker.add(Duration.ofMinutes(2))
    service.getCourtRegisterByCourtCodeCached("COURT1")
    verify(exactly = 2) { client.getCourtRegister("COURT1") }
  }

  @Test
  fun `should ensure failed lookups are not cached so the next call retries`() {
    val court = CourtRegister("COURT1", "Court", "Description")
    every { client.getCourtRegister("COURT1") } throws RuntimeException("boom") andThen court

    assertThat(service.getCourtRegisterByCourtCodeCached("COURT1")).isNull()
    assertThat(service.getCourtRegisterByCourtCodeCached("COURT1")).isEqualTo(court)
    assertThat(service.getCourtRegisterByCourtCodeCached("COURT1")).isEqualTo(court)

    verify(exactly = 2) { client.getCourtRegister("COURT1") }
  }
}
