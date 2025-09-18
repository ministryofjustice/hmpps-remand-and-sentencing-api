package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.listener.dto.PrisonerBookingMovedEvent
import java.time.ZonedDateTime

@Service
class PrisonerEventService(private val courtCaseRepository: CourtCaseRepository, private val courtCaseHistoryRepository: CourtCaseHistoryRepository) {
  @Transactional
  fun handleBookingMoved(event: PrisonerBookingMovedEvent) {
    log.info("handling booking moved from {} to {}", event.additionalInformation.movedFromNomsNumber, event.additionalInformation.movedToNomsNumber)
    val courtCases = courtCaseRepository.findByPrisonerIdAndBookingId(event.additionalInformation.movedFromNomsNumber, event.additionalInformation.bookingId)
    courtCases.forEach { courtCase ->
      courtCase.prisonerId = event.additionalInformation.movedToNomsNumber
      courtCase.updatedAt = ZonedDateTime.now()
      courtCase.updatedBy = "NOMIS"
      courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(courtCase))
    }
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
