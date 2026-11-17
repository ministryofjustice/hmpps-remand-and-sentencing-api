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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails
import java.time.Duration

@SpringJUnitConfig(CachingTestConfig::class, ManageOffencesService::class)
class ManageOffencesServiceCachingTest {

  @Autowired lateinit var cacheManager: CacheManager

  @Autowired lateinit var ticker: UnitTestTicker

  @Autowired lateinit var client: ManageOffencesApiClient

  @Autowired lateinit var service: ManageOffencesService

  @BeforeEach
  fun reset() {
    clearAllMocks()
    cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
  }

  @Test
  fun `should ensure lookups are cached per offence code`() {
    every { client.getOffenceDetails(any()) } answers { OffenceDetails(1, firstArg(), "Description") }

    service.getOffenceDetailsCached("AA06027")
    service.getOffenceDetailsCached("AA06027")
    service.getOffenceDetailsCached("AA06028")

    verify(exactly = 1) { client.getOffenceDetails("AA06027") }
    verify(exactly = 1) { client.getOffenceDetails("AA06028") }
  }

  @Test
  fun `should ensure entries are evicted once the ttl has passed`() {
    every { client.getOffenceDetails("AA06027") } returns OffenceDetails(1, "AA06027", "Description")

    service.getOffenceDetailsCached("AA06027")
    ticker.add(Duration.ofMinutes(14))
    service.getOffenceDetailsCached("AA06027")
    verify(exactly = 1) { client.getOffenceDetails("AA06027") }

    ticker.add(Duration.ofMinutes(2))
    service.getOffenceDetailsCached("AA06027")
    verify(exactly = 2) { client.getOffenceDetails("AA06027") }
  }

  @Test
  fun `should ensure failed lookups are not cached so the next call retries`() {
    val offence = OffenceDetails(1, "AA06027", "Description")
    every { client.getOffenceDetails("AA06027") } throws RuntimeException("boom") andThen offence

    assertThat(service.getOffenceDetailsCached("AA06027")).isNull()
    assertThat(service.getOffenceDetailsCached("AA06027")).isEqualTo(offence)
    assertThat(service.getOffenceDetailsCached("AA06027")).isEqualTo(offence)

    verify(exactly = 2) { client.getOffenceDetails("AA06027") }
  }
}
