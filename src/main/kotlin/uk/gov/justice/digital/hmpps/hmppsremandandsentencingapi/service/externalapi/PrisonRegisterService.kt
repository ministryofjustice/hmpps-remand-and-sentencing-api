package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails

@Service
class PrisonRegisterService(private val prisonRegisterClient: PrisonRegisterClient) {

  fun getAgencyDetails(agencyId: String): AgencyDetails? {
    try {
      return prisonRegisterClient.getAgencyDetails(agencyId)
    } catch (e: Exception) {
      log.error("Unable to retrieve agency details for agency id {}. API lookup failed.", agencyId)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
