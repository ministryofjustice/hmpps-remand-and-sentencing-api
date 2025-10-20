package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.listener.dto.PrisonerBookingMovedEvent
import java.time.ZonedDateTime

@Service
class PrisonerEventService(
  private val courtCaseRepository: CourtCaseRepository,
  private val courtCaseHistoryRepository: CourtCaseHistoryRepository,
  private val recallRepository: RecallRepository,
  private val recallHistoryRepository: RecallHistoryRepository,
) {
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

    handleBookingMovedForRecalls(event)
  }

  private fun handleBookingMovedForRecalls(event: PrisonerBookingMovedEvent) {
    val recalls = recallRepository.findByPrisonerIdAndBookingId(
      prisonerId = event.additionalInformation.movedFromNomsNumber,
      bookingId = event.additionalInformation.bookingId,
    )

    val now = ZonedDateTime.now()
    recalls.forEach { recall ->
      recall.prisonerId = event.additionalInformation.movedToNomsNumber
      recall.updatedAt = now
      recall.updatedBy = "NOMIS"
      recallHistoryRepository.save(RecallHistoryEntity.from(recall, RecallEntityStatus.EDITED))
    }

    log.info("Updated {} recalls for bookingId={}", recalls.size, event.additionalInformation.bookingId)
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
