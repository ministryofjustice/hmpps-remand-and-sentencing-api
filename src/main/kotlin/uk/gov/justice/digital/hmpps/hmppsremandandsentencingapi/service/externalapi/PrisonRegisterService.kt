package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants.Companion.PRISON_REGISTER_AGENCY_DETAILS_CACHE_KEY

@Service
class PrisonRegisterService(private val prisonRegisterClient: PrisonRegisterClient) {

  @Cacheable(PRISON_REGISTER_AGENCY_DETAILS_CACHE_KEY, key = "#agencyId", unless = "#result == null")
  fun getAgencyDetailsCached(agencyId: String): AgencyDetails? {
    try {
      return prisonRegisterClient.getAgencyDetails(agencyId)
    } catch (e: Exception) {
      log.error("Unable to retrieve agency details for agency id {}. API lookup failed.", agencyId)
      log.debug(e.message)
    }
    return null
  }

  fun getAgencyDetails(agencyId: String): AgencyDetails? {
    try {
      return prisonRegisterClient.getAgencyDetails(agencyId)
    } catch (e: Exception) {
      log.error("Unable to retrieve agency details for agency id {}. API lookup failed.", agencyId)
      log.debug(e.message)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
