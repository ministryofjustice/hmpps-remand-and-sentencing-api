package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.admin.FixSingleSentenceMultipleChargesPeople
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository

@Service
class BulkFixManyChargesToSentenceService(
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService,
  private val courtCaseRepository: CourtCaseRepository,
  private val dpsDomainEventService: DpsDomainEventService,
) {

  @Async
  fun fixCourtCaseSentences(limit: Int) {
    log.info("Starting Bulk Fix Many Charges to Single Sentence async job with limit {}", limit)
    val events = fixCourtCases(limit)
    log.info(
      "Completed Bulk Fix Many Charges to Single Sentence for {} affected court cases. Emitted a total of {} events",
      limit,
      events.size,
    )
    dpsDomainEventService.emitEvents(events)
  }

  @Transactional
  fun fixCourtCases(limit: Int): Set<EventMetadata> {
    val courtCaseUuids = courtCaseRepository.findIdWithManyChargesDataFixByConsecutiveToLast(limit)
    return fixManyChargesToSentenceService.fixCourtCasesById(courtCaseUuids, "BATCH_JOB")
  }

  @Async
  fun fixPeople(fixSingleSentenceMultipleChargesPeople: FixSingleSentenceMultipleChargesPeople, username: String) {
    log.info("Starting Bulk Fix Many Charges to Single Sentence people async job")
    val events = fixPrisoners(fixSingleSentenceMultipleChargesPeople.prisonerIds, username)
    dpsDomainEventService.emitEvents(events)
    log.info("Completed Bulk Fix Many Charges to Single Sentence people async job. Emitted a total of {} events", events.size)
  }

  @Transactional
  fun fixPrisoners(prisonerIds: List<String>, username: String): Set<EventMetadata> = prisonerIds.flatMap { fixManyChargesToSentenceService.fixPrisoner(it, username) }.toSet()

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
