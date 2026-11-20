package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants.Companion.MANAGE_OFFENCE_DETAILS_CACHE_KEY

@Service
class ManageOffencesService(private val manageOffencesApiClient: ManageOffencesApiClient) {

  @Cacheable(MANAGE_OFFENCE_DETAILS_CACHE_KEY, key = "#offenceCode", unless = "#result == null")
  fun getOffenceDetailsCached(offenceCode: String): OffenceDetails? {
    try {
      return manageOffencesApiClient.getOffenceDetails(offenceCode)
    } catch (e: Exception) {
      log.error("Unable to retrieve offence details for offence code {}. API lookup failed.", offenceCode)
      log.debug(e.message)
    }
    return null
  }

  fun getOffenceDetails(offenceCode: String): OffenceDetails? {
    try {
      return manageOffencesApiClient.getOffenceDetails(offenceCode)
    } catch (e: Exception) {
      log.error("Unable to retrieve offence details for offence code {}. API lookup failed.", offenceCode)
      log.debug(e.message)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
