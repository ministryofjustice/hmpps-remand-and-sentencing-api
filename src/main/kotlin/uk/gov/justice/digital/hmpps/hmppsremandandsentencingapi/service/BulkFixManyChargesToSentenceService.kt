package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository

@Service
class BulkFixManyChargesToSentenceService(
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService,
  private val courtCaseRepository: CourtCaseRepository,
  private val dpsDomainEventService: DpsDomainEventService,
) {

  @Async
  @Transactional
  fun fixCourtCaseSentences(limit: Int) {
    log.info("Starting Bulk Fix Many Charges to Single Sentence async job with limit {}", limit)
    val events = mutableSetOf<EventMetadata>()
    val courtCaseUuids = courtCaseRepository.findIdWithManyChargesDataFixByUpdatedAtDesc(limit)
    events.addAll(fixManyChargesToSentenceService.fixCourtCasesById(courtCaseUuids, "BATCH_JOB"))
    log.info("Completed Bulk Fix Many Charges to Single Sentence for {} affected court cases. Emitted a total of {} events", limit, events.size)
    dpsDomainEventService.emitEvents(events)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
