package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ImmigrationDetentionHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ImmigrationDetentionRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ImmigrationDetentionHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.listener.dto.PrisonerBookingMovedEvent
import java.time.ZonedDateTime

@Service
class PrisonerEventService(
  private val courtCaseRepository: CourtCaseRepository,
  private val courtCaseHistoryRepository: CourtCaseHistoryRepository,
  private val recallRepository: RecallRepository,
  private val recallHistoryRepository: RecallHistoryRepository,
  private val immigrationDetentionRepository: ImmigrationDetentionRepository,
  private val immigrationDetentionHistoryRepository: ImmigrationDetentionHistoryRepository,
) {
  @Transactional
  fun handleBookingMoved(event: PrisonerBookingMovedEvent) {
    log.info("handling booking moved from {} to {}", event.additionalInformation.movedFromNomsNumber, event.additionalInformation.movedToNomsNumber)
    val courtCases = courtCaseRepository.findByPrisonerIdAndBookingId(event.additionalInformation.movedFromNomsNumber, event.additionalInformation.bookingId)
    courtCases.forEach { courtCase ->
      courtCase.prisonerId = event.additionalInformation.movedToNomsNumber
      courtCase.updatedAt = ZonedDateTime.now()
      courtCase.updatedBy = "NOMIS"
      courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(courtCase, ChangeSource.NOMIS))
    }

    handleBookingMovedForRecalls(event)
    handleBookingMovedForImmigrationDetention(courtCases, event)
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
      recallHistoryRepository.save(RecallHistoryEntity.from(recall, ChangeSource.NOMIS))
    }

    log.info("Updated {} recalls for bookingId={}", recalls.size, event.additionalInformation.bookingId)
  }

  private fun handleBookingMovedForImmigrationDetention(courtCases: List<CourtCaseEntity>, event: PrisonerBookingMovedEvent) {
    val courtAppearanceUuids = courtCases.flatMap { it.appearances.filter { it.statusId != CourtAppearanceEntityStatus.DELETED }.map { it.appearanceUuid } }
    val immigrationDetentions = immigrationDetentionRepository.findByCourtAppearanceUuidInAndStatusId(courtAppearanceUuids)
    val now = ZonedDateTime.now()
    immigrationDetentions.forEach { immigrationDetention ->
      immigrationDetention.prisonerId = event.additionalInformation.movedToNomsNumber
      immigrationDetention.updatedAt = now
      immigrationDetention.updatedBy = "NOMIS"
      immigrationDetentionHistoryRepository.save(ImmigrationDetentionHistoryEntity.from(immigrationDetention))
    }
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
