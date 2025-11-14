package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceTypesService

@EnableCaching
@Configuration
class CacheConfiguration {

  @Bean
  fun fillLegacySentenceTypeCache(legacySentenceTypesService: LegacySentenceTypesService): CommandLineRunner = CommandLineRunner {
    log.info("Calling fillLegacySentenceTypeCache")
    val allLegacySentenceTypes = legacySentenceTypesService.getAllLegacySentences()
    allLegacySentenceTypes.map { it.nomisSentenceTypeReference }.distinct().forEach { legacySentenceTypesService.getLegacySentencesByNomisSentenceTypeReferenceAsSummary(it) }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
