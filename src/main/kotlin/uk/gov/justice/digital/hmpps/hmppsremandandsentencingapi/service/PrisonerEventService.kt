package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.listener.dto.PrisonerBookingMovedEvent

@Service
class PrisonerEventService {
  fun handleBookingMoved(event: PrisonerBookingMovedEvent) {
    log.info("handling booking moved from {} to {}", event.additionalInformation.movedFromNomsNumber, event.additionalInformation.movedToNomsNumber)
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
