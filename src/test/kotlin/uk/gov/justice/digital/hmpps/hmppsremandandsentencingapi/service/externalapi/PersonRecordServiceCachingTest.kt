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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PersonRecordClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison
import java.time.Duration

@SpringJUnitConfig(CachingTestConfig::class, PersonRecordService::class)
class PersonRecordServiceCachingTest {

  @Autowired lateinit var cacheManager: CacheManager

  @Autowired lateinit var ticker: UnitTestTicker

  @Autowired lateinit var client: PersonRecordClient

  @Autowired lateinit var service: PersonRecordService

  @BeforeEach
  fun reset() {
    clearAllMocks()
    cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
  }

  @Test
  fun `should ensure lookups are cached per prisoner`() {
    every { client.getPersonPrison(any()) } returns PersonPrison("Joe", "Bloggs", null)

    service.getPersonPrisonCached("A1234BC")
    service.getPersonPrisonCached("A1234BC")
    service.getPersonPrisonCached("B1234CD")

    verify(exactly = 1) { client.getPersonPrison("A1234BC") }
    verify(exactly = 1) { client.getPersonPrison("B1234CD") }
  }

  @Test
  fun `should ensure entries are evicted once the ttl has passed`() {
    every { client.getPersonPrison("A1234BC") } returns PersonPrison("Joe", "Bloggs", null)

    service.getPersonPrisonCached("A1234BC")
    ticker.add(Duration.ofMinutes(14))
    service.getPersonPrisonCached("A1234BC")
    verify(exactly = 1) { client.getPersonPrison("A1234BC") }

    ticker.add(Duration.ofMinutes(2))
    service.getPersonPrisonCached("A1234BC")
    verify(exactly = 2) { client.getPersonPrison("A1234BC") }
  }

  @Test
  fun `should ensure failed lookups are not cached so the next call retries`() {
    val person = PersonPrison("Joe", "Bloggs", null)
    every { client.getPersonPrison("A1234BC") } throws RuntimeException("boom") andThen person

    assertThat(service.getPersonPrisonCached("A1234BC")).isNull()
    assertThat(service.getPersonPrisonCached("A1234BC")).isEqualTo(person)
    assertThat(service.getPersonPrisonCached("A1234BC")).isEqualTo(person)

    verify(exactly = 2) { client.getPersonPrison("A1234BC") }
  }
}
