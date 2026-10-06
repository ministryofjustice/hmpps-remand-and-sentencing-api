package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails

@ConditionalOnProperty(
  prefix = "features.client-api.prison-register",
  name = ["enabled"],
  havingValue = "false",
  matchIfMissing = true,
)
@Component
class PrisonRegisterClientNoop : PrisonRegisterClient {

  override fun getAgencyDetails(agencyId: String): AgencyDetails? = null
}
