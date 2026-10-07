package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails

interface PrisonRegisterClient {

  fun getAgencyDetails(agencyId: String): AgencyDetails?
}
