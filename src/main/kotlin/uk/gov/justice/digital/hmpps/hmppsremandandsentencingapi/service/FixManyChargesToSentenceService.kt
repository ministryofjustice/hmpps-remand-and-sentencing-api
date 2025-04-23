package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

@Service
class FixManyChargesToSentenceService(private val sentenceHistoryRepository: SentenceHistoryRepository, private val periodLengthHistoryRepository: PeriodLengthHistoryRepository) {

  fun fixCourtCaseSentences(courtCases: List<CourtCaseEntity>): MutableSet<EventMetadata> = courtCases.flatMap { courtCase ->
    fixSentences(courtCaseToSentences(courtCase))
  }.toMutableSet()

  private fun courtCaseToSentences(courtCaseEntity: CourtCaseEntity): List<RecordEventMetadata<SentenceEntity>> = courtCaseEntity.appearances.flatMap { appearance ->
    appearance.appearanceCharges.filter { it.charge != null }.flatMap { appearanceCharge ->
      appearanceCharge.charge!!.sentences.map { sentenceEntity ->
        RecordEventMetadata(
          sentenceEntity,
          EventMetadata(courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, appearance.appearanceUuid.toString(), appearanceCharge.charge!!.chargeUuid.toString(), null, null, EventType.METADATA_ONLY),
        )
      }
    }
  }

  fun fixSentences(sentences: List<RecordEventMetadata<SentenceEntity>>): MutableSet<EventMetadata> {
    val toFixSentences = sentences
      .filter { it.record.statusId == EntityStatus.MANY_CHARGES_DATA_FIX }
      .groupByTo(mutableMapOf()) { it.record.sentenceUuid }
    val eventsToEmit = mutableSetOf<EventMetadata>()
    toFixSentences.forEach { (_, sentenceRecords) ->
      val (firstRecord, eventMetadata) = sentenceRecords.removeFirst()
      firstRecord.statusId = if (firstRecord.legacyData?.active == false) EntityStatus.INACTIVE else EntityStatus.ACTIVE
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(firstRecord))
      firstRecord.periodLengths.filter { it.statusId == EntityStatus.MANY_CHARGES_DATA_FIX }
        .forEach { firstRecordPeriodLength ->
          firstRecordPeriodLength.statusId = EntityStatus.ACTIVE
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(firstRecordPeriodLength))
        }

      eventsToEmit.add(
        EventMetadataCreator.sentenceEventMetadata(
          eventMetadata.prisonerId,
          eventMetadata.courtCaseId!!,
          eventMetadata.chargeId!!,
          firstRecord.sentenceUuid.toString(),
          eventMetadata.courtAppearanceId!!,
          EventType.SENTENCE_UPDATED,
        ),
      )

      sentenceRecords.forEach { (sentenceRecord, eventMetadata) ->
        sentenceRecord.sentenceUuid = UUID.randomUUID()
        sentenceRecord.statusId = if (sentenceRecord.legacyData?.active == false) EntityStatus.INACTIVE else EntityStatus.ACTIVE
        sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentenceRecord))
        eventsToEmit.add(
          EventMetadataCreator.sentenceEventMetadata(
            eventMetadata.prisonerId,
            eventMetadata.courtCaseId!!,
            eventMetadata.chargeId!!,
            sentenceRecord.sentenceUuid.toString(),
            eventMetadata.courtAppearanceId!!,
            EventType.SENTENCE_INSERTED,
          ),
        )
        sentenceRecord.periodLengths.forEach { periodLengthRecord ->
          periodLengthRecord.periodLengthUuid = UUID.randomUUID()
          periodLengthRecord.statusId = EntityStatus.ACTIVE
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLengthRecord))
        }
      }
    }
    return eventsToEmit
  }
}
