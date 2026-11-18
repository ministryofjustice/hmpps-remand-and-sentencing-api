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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails
import java.time.Duration

@SpringJUnitConfig(CachingTestConfig::class, PrisonRegisterService::class)
class PrisonRegisterServiceCachingTest {

  @Autowired lateinit var cacheManager: CacheManager

  @Autowired lateinit var ticker: UnitTestTicker

  @Autowired lateinit var client: PrisonRegisterClient

  @Autowired lateinit var service: PrisonRegisterService

  private fun agency(id: String) = AgencyDetails(id, "Description", "INST", true, emptyList(), emptyList())

  @BeforeEach
  fun reset() {
    clearAllMocks()
    cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
  }

  @Test
  fun `should ensure lookups are cached per agency`() {
    every { client.getAgencyDetails(any()) } answers { agency(firstArg()) }

    service.getAgencyDetailsCached("KMI")
    service.getAgencyDetailsCached("KMI")
    service.getAgencyDetailsCached("LEI")

    verify(exactly = 1) { client.getAgencyDetails("KMI") }
    verify(exactly = 1) { client.getAgencyDetails("LEI") }
  }

  @Test
  fun `should ensure entries are evicted once the ttl has passed`() {
    every { client.getAgencyDetails("KMI") } returns agency("KMI")

    service.getAgencyDetailsCached("KMI")
    ticker.add(Duration.ofMinutes(14))
    service.getAgencyDetailsCached("KMI")
    verify(exactly = 1) { client.getAgencyDetails("KMI") }

    ticker.add(Duration.ofMinutes(2))
    service.getAgencyDetailsCached("KMI")
    verify(exactly = 2) { client.getAgencyDetails("KMI") }
  }

  @Test
  fun `should ensure failed lookups are not cached so the next call retries`() {
    every { client.getAgencyDetails("KMI") } throws RuntimeException("boom") andThen agency("KMI")

    assertThat(service.getAgencyDetailsCached("KMI")).isNull()
    assertThat(service.getAgencyDetailsCached("KMI")).isEqualTo(agency("KMI"))
    assertThat(service.getAgencyDetailsCached("KMI")).isEqualTo(agency("KMI"))

    verify(exactly = 2) { client.getAgencyDetails("KMI") }
  }
}
