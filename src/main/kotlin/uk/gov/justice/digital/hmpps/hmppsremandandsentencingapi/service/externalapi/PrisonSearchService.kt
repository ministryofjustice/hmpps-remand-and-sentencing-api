package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails

@Component
class PrisonSearchService(private val prisonerSearchClient: PrisonerSearchClient) {

  @Cacheable("prisonSearchGetPersoner")
  fun getPrisonerCached(prisonerId: String): PrisonSearchDetails? {
    try {
      return prisonerSearchClient.getPrisoner(prisonerId)
    } catch (e: Exception) {
      log.error("Unable to retrieve prisoner search details for prisoner id {}. API lookup failed.", prisonerId)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
