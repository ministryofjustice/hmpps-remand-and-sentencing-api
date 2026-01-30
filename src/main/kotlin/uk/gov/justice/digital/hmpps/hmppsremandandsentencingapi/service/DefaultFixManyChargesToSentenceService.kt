package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.time.ZonedDateTime
import java.util.UUID

@ConditionalOnProperty(
  name = ["features.fix.many.charges.to.sentence"],
  havingValue = "enabled",
)
@Service
class DefaultFixManyChargesToSentenceService(private val sentenceHistoryRepository: SentenceHistoryRepository, private val periodLengthHistoryRepository: PeriodLengthHistoryRepository, private val serviceUserService: ServiceUserService, private val courtCaseRepository: CourtCaseRepository, private val sentenceRepository: SentenceRepository) : FixManyChargesToSentenceService {

  override fun fixCourtCaseSentences(courtCases: List<CourtCaseEntity>): MutableSet<EventMetadata> = courtCases.flatMap { courtCase ->
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

  override fun fixCourtCasesById(courtCaseIds: Set<Int>): MutableSet<EventMetadata> = courtCaseRepository.findAllById(courtCaseIds).flatMap { courtCase ->
    fixSentences(courtCaseToSentences(courtCase))
  }.toMutableSet()

  override fun fixSentencesBySentenceUuids(sentenceUuids: List<RecordEventMetadata<UUID>>): MutableSet<EventMetadata> = fixSentences(sentenceRepository.findBySentenceUuidIn(sentenceUuids.map { it.record }).map { sentenceEntity -> sentenceUuids.first { it.record == sentenceEntity.sentenceUuid }.toNewRecord(sentenceEntity) })

  override fun fixSentences(sentences: List<RecordEventMetadata<SentenceEntity>>): MutableSet<EventMetadata> {
    val toFixSentences = sentences
      .filter { it.record.statusId == SentenceEntityStatus.MANY_CHARGES_DATA_FIX }
      .groupByTo(mutableMapOf()) { it.record.sentenceUuid }
    val eventsToEmit = mutableSetOf<EventMetadata>()
    toFixSentences.forEach { (originalSentenceUuid, sentenceRecords) ->
      val firstSentenceRecordEventMetadata = sentenceRecords.removeFirst()
      val firstSentenceEventToEmit = fixSentence(firstSentenceRecordEventMetadata, EventType.SENTENCE_UPDATED, originalSentenceUuid)
      eventsToEmit.add(firstSentenceEventToEmit)
      fixPeriodLengths(firstSentenceRecordEventMetadata)

      sentenceRecords.forEach { sentenceRecordEventMetadata ->
        val sentenceEventToEmit = fixSentence(sentenceRecordEventMetadata, EventType.SENTENCE_FIX_SINGLE_CHARGE_INSERTED, originalSentenceUuid) {
          it.sentenceUuid = UUID.randomUUID()
          it.legacyData?.nomisLineReference = null
        }
        eventsToEmit.add(sentenceEventToEmit)
        val periodLengthEventsToEmit = fixPeriodLengths(sentenceRecordEventMetadata) { it.periodLengthUuid = UUID.randomUUID() }
        eventsToEmit.addAll(periodLengthEventsToEmit)
      }
    }
    return eventsToEmit
  }

  private fun fixSentence(sentenceRecordEventMetadata: RecordEventMetadata<SentenceEntity>, sentenceEventType: EventType, originalSentenceUuid: UUID, sentenceModifyFunction: (SentenceEntity) -> Unit = {}): EventMetadata {
    val (sentenceRecord, eventMetadata) = sentenceRecordEventMetadata
    sentenceRecord.statusId = if (sentenceRecord.legacyData?.active == false) SentenceEntityStatus.INACTIVE else SentenceEntityStatus.ACTIVE
    sentenceRecord.updatedAt = ZonedDateTime.now()
    sentenceRecord.updatedBy = serviceUserService.getUsername()
    sentenceModifyFunction(sentenceRecord)
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentenceRecord, ChangeSource.DPS))
    return EventMetadataCreator.fixSentenceEventMetadata(
      eventMetadata.prisonerId,
      eventMetadata.courtCaseId!!,
      eventMetadata.chargeId!!,
      sentenceRecord.sentenceUuid.toString(),
      eventMetadata.courtAppearanceId!!,
      sentenceEventType,
      originalSentenceUuid.toString(),
    )
  }

  private fun fixPeriodLengths(sentenceRecordEventMetadata: RecordEventMetadata<SentenceEntity>, periodLengthModifyFunction: (PeriodLengthEntity) -> Unit = {}): MutableSet<EventMetadata> {
    val (sentenceRecord, eventMetadata) = sentenceRecordEventMetadata
    val periodLengthEventsToEmit = mutableSetOf<EventMetadata>()
    val periodLengths = sentenceRecordEventMetadata.record.periodLengths
    periodLengths.filter { it.statusId == PeriodLengthEntityStatus.MANY_CHARGES_DATA_FIX }
      .forEach { periodLength ->
        periodLength.statusId = PeriodLengthEntityStatus.ACTIVE
        periodLength.updatedAt = ZonedDateTime.now()
        periodLength.updatedBy = serviceUserService.getUsername()
        periodLengthModifyFunction(periodLength)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength, ChangeSource.DPS))
        periodLengthEventsToEmit.add(
          EventMetadataCreator.periodLengthEventMetadata(
            eventMetadata.prisonerId,
            eventMetadata.courtCaseId!!,
            eventMetadata.courtAppearanceId!!,
            eventMetadata.chargeId!!,
            sentenceRecord.sentenceUuid.toString(),
            periodLength.periodLengthUuid.toString(),
            EventType.PERIOD_LENGTH_INSERTED,
          ),
        )
      }
    return periodLengthEventsToEmit
  }
}
