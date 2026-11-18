package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.springframework.beans.factory.ObjectProvider
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.cache.CacheExpiry

@EnableCaching
@Configuration
class CacheConfiguration(
  cacheExpiryList: List<CacheExpiry>,
  tickerProvider: ObjectProvider<Ticker>,
) {
  private val ticker: Ticker = tickerProvider.getIfAvailable(Ticker::systemTicker)
  private val expiryByCacheMap: Map<String, CacheExpiry> = cacheExpiryList.associateBy { it.cacheName }

  @Bean
  fun cacheManager(): CacheManager = object : CaffeineCacheManager() {
    override fun createCaffeineCache(name: String): Cache = CaffeineCache(name, buildCaffeineSpec(name).build(), true)
  }

  private fun buildCaffeineSpec(cacheName: String): Caffeine<Any, Any> {
    val builder = Caffeine.newBuilder().ticker(ticker)
    expiryByCacheMap[cacheName]?.let { builder.maximumSize(it.maximumSize).expireAfter(it) }
    return builder
  }
}
