package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails

@Service
class ManageOffencesService(private val manageOffencesApiClient: ManageOffencesApiClient) {

  fun getOffenceDetails(offenceCode: String): OffenceDetails? {
    try {
      return manageOffencesApiClient.getOffenceDetails(offenceCode)
    } catch (e: Exception) {
      log.error("Unable to retrieve offence details for offence code {}. API lookup failed.", offenceCode)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
