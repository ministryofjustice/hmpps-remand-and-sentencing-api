package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister

@Service
class CourtRegisterService(private val courtRegisterApiClient: CourtRegisterApiClient) {

  @Cacheable("courtRegisterByCourtCode")
  fun getCourtRegisterByCourtCodeCached(courtCode: String): CourtRegister? {
    try {
      return courtRegisterApiClient.getCourtRegister(courtCode)
    } catch (e: WebClientException) {
      log.error("Unable to retrieve court name for courtCode {}. API lookup failed.", courtCode)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
