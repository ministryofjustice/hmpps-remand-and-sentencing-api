package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

@Service
class FixManyChargesToSentenceService(private val sentenceHistoryRepository: SentenceHistoryRepository, private val periodLengthHistoryRepository: PeriodLengthHistoryRepository) {

  fun fixSentences(sentences: List<SentenceEntity>) {
    val toFixSentences = sentences
      .filter { it.statusId == EntityStatus.MANY_CHARGES_DATA_FIX }
      .groupByTo(mutableMapOf()) { it.sentenceUuid }

    toFixSentences.forEach { (sentenceUuid, sentenceRecords) ->
      val firstRecord = sentenceRecords.removeFirst()
      firstRecord.statusId = EntityStatus.ACTIVE
      sentenceHistoryRepository.save(SentenceHistoryEntity.Companion.from(firstRecord))
      firstRecord.periodLengths.filter { it.statusId == EntityStatus.MANY_CHARGES_DATA_FIX }
        .forEach { firstRecordPeriodLength ->
          firstRecordPeriodLength.statusId = EntityStatus.ACTIVE
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.Companion.from(firstRecordPeriodLength))
        }

      sentenceRecords.forEach { sentenceRecord ->
        sentenceRecord.sentenceUuid = UUID.randomUUID()
        sentenceRecord.statusId = EntityStatus.ACTIVE
        sentenceHistoryRepository.save(SentenceHistoryEntity.Companion.from(sentenceRecord))
        sentenceRecord.periodLengths.forEach { periodLengthRecord ->
          periodLengthRecord.periodLengthUuid = UUID.randomUUID()
          periodLengthRecord.statusId = EntityStatus.ACTIVE
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.Companion.from(periodLengthRecord))
        }
      }
    }
  }
}
