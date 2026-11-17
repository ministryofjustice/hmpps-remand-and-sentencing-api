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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails
import java.time.Duration
import java.time.LocalDate

@SpringJUnitConfig(CachingTestConfig::class, PrisonSearchService::class)
class PrisonSearchServiceCachingTest {

  @Autowired lateinit var cacheManager: CacheManager

  @Autowired lateinit var ticker: UnitTestTicker

  @Autowired lateinit var client: PrisonerSearchClient

  @Autowired lateinit var service: PrisonSearchService

  private val prisoner = PrisonSearchDetails("KMI", "Kirkham (HMP)", LocalDate.of(1990, 1, 1))

  @BeforeEach
  fun reset() {
    clearAllMocks()
    cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
  }

  @Test
  fun `should ensure lookups are cached per prisoner`() {
    every { client.getPrisoner(any()) } returns prisoner

    service.getPrisonerCached("A1234BC")
    service.getPrisonerCached("A1234BC")
    service.getPrisonerCached("B1234CD")

    verify(exactly = 1) { client.getPrisoner("A1234BC") }
    verify(exactly = 1) { client.getPrisoner("B1234CD") }
  }

  @Test
  fun `should ensure entries are evicted once the ttl has passed`() {
    every { client.getPrisoner("A1234BC") } returns prisoner

    service.getPrisonerCached("A1234BC")
    ticker.add(Duration.ofMinutes(14))
    service.getPrisonerCached("A1234BC")
    verify(exactly = 1) { client.getPrisoner("A1234BC") }

    ticker.add(Duration.ofMinutes(2))
    service.getPrisonerCached("A1234BC")
    verify(exactly = 2) { client.getPrisoner("A1234BC") }
  }

  @Test
  fun `should ensure failed lookups are not cached so the next call retries`() {
    every { client.getPrisoner("A1234BC") } throws RuntimeException("boom") andThen prisoner

    assertThat(service.getPrisonerCached("A1234BC")).isNull()
    assertThat(service.getPrisonerCached("A1234BC")).isEqualTo(prisoner)
    assertThat(service.getPrisonerCached("A1234BC")).isEqualTo(prisoner)

    verify(exactly = 2) { client.getPrisoner("A1234BC") }
  }
}
