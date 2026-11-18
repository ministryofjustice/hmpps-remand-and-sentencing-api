package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache.CacheExpiry
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PersonRecordClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.CacheConfiguration

@TestConfiguration
@Import(CacheConfiguration::class)
@ComponentScan(basePackageClasses = [CacheExpiry::class])
class CachingTestConfig {
  @Bean
  fun ticker(): UnitTestTicker = UnitTestTicker()

  @Bean
  fun courtRegisterApiClient(): CourtRegisterApiClient = mockk()

  @Bean
  fun manageOffencesApiClient(): ManageOffencesApiClient = mockk()

  @Bean
  fun personRecordClient(): PersonRecordClient = mockk()

  @Bean
  fun prisonRegisterClient(): PrisonRegisterClient = mockk()

  @Bean
  fun prisonerSearchClient(): PrisonerSearchClient = mockk()
}
