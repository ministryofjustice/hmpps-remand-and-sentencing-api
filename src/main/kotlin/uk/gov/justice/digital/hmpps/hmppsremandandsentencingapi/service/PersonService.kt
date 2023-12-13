package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PersonDetails

@Service
class PersonService(private val prisonApiClient: PrisonApiClient) {

  fun getPersonDetailsByPrisonerId(prisonerId: String): PersonDetails =
    prisonApiClient.getOffenderDetail(prisonerId).let { PersonDetails.from(it) }
}
