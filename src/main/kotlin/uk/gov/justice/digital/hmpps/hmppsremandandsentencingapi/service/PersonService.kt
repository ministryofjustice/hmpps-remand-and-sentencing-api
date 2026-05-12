package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PersonDetails

@Service
class PersonService(private val prisonApiClient: PrisonApiClient) {

  fun getPersonDetailsByPrisonerId(prisonerId: String): PersonDetails = prisonApiClient.getOffenderDetail(prisonerId).let { PersonDetails.from(it) }

  @Cacheable("personDetailsByPrisonerId")
  fun getPersonDetailsByPrisonerIdCached(prisonerId: String): PersonDetails? {
    try {
      return prisonApiClient.getOffenderDetail(prisonerId).let { PersonDetails.from(it) }
    } catch (e: Exception) {
      log.error("Unable to retrieve person details for prisonerId {}. API lookup failed.", prisonerId)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
