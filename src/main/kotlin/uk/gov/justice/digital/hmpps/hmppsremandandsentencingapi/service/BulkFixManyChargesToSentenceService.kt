package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository

@Service
class BulkFixManyChargesToSentenceService(
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService,
  private val courtCaseRepository: CourtCaseRepository,
) {

  @Transactional
  fun fixCourtCaseSentences(limit: Int): Set<EventMetadata> {
    val events = mutableSetOf<EventMetadata>()
    courtCaseRepository.findCaseUniqueIdentifierWithManyChargesDataFixByUpdatedAtDesc(limit).forEach { courtCaseUuid ->
      courtCaseRepository.findSentencedCourtCase(courtCaseUuid)?.let { courtCaseEntity ->
        events.addAll(fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity, "BATCH_JOB"))
      }
    }
    return events
  }
}
