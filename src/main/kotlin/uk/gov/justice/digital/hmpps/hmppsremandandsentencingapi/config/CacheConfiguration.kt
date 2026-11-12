package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache.NextCourtAppearancePersonCacheExpiry
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants

@EnableCaching
@Configuration
class CacheConfiguration {
  @Bean
  fun cacheManager(): CacheManager = object : CaffeineCacheManager() {
    override fun createCaffeineCache(name: String): Cache = CaffeineCache(name, buildCaffeineSpec(name).build(), true)
  }

  private fun buildCaffeineSpec(cacheName: String): Caffeine<Any, Any> {
    val builder: Caffeine<Any, Any> = Caffeine.newBuilder()
    if (cacheName == Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY) {
      builder.maximumSize(100_000).expireAfter(NextCourtAppearancePersonCacheExpiry())
    }

    return builder
  }
}
