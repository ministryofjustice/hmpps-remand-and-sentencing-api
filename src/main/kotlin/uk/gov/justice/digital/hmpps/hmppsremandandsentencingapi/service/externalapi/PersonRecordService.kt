package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PersonRecordClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison

@Service
class PersonRecordService(private val personRecordClient: PersonRecordClient) {

  @Cacheable("prisonRegisterGetAgencyDetails")
  fun getPersonPrison(prisonerId: String): PersonPrison? {
    try {
      return personRecordClient.getPersonPrison(prisonerId)
    } catch (e: Exception) {
      log.error("Unable to retrieve offence details for prisoner id {}. API lookup failed.", prisonerId)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
