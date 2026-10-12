package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.hibernate.exception.LockAcquisitionException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class FixManyChargesToSentenceService(
  private val sentenceHistoryRepository: SentenceHistoryRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
  private val courtCaseRepository: CourtCaseRepository,
  private val sentenceRepository: SentenceRepository,
  private val courtCaseHistoryRepository: CourtCaseHistoryRepository,
) {
  @Transactional
  fun fixPrisoner(prisonerId: String, username: String? = null): MutableSet<EventMetadata> = courtCaseRepository.findAllByPrisonerIdAndStatusIdNot(prisonerId).flatMap { courtCase ->
    fixSentences(courtCaseToSentences(courtCase), username)
  }.toMutableSet()

  fun fixCourtCaseSentences(courtCase: CourtCaseEntity): MutableSet<EventMetadata> = fixSentences(courtCaseToSentences(courtCase))

  fun fixCourtCaseSentences(courtCases: List<CourtCaseEntity>): MutableSet<EventMetadata> = courtCases.flatMap { courtCase ->
    fixSentences(courtCaseToSentences(courtCase))
  }.toMutableSet()

  private fun courtCaseToSentences(courtCaseEntity: CourtCaseEntity): List<FixSentenceHierarchy> = courtCaseEntity.appearances.flatMap { appearance ->
    appearance.appearanceCharges.filter { it.charge != null }.flatMap { appearanceCharge ->
      appearanceCharge.charge!!.sentences.map { sentenceEntity ->
        FixSentenceHierarchy(courtCaseEntity, appearance.appearanceUuid.toString(), appearanceCharge.charge!!.chargeUuid.toString(), sentenceEntity)
      }
    }
  }

  @Retryable(maxAttempts = 3, retryFor = [ObjectOptimisticLockingFailureException::class, CannotAcquireLockException::class, LockAcquisitionException::class], backoff = Backoff(delay = 500))
  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
  fun fixCourtCasesById(courtCaseIds: Set<Int>, username: String? = null): MutableSet<EventMetadata> = courtCaseRepository.findAllById(courtCaseIds).flatMap { courtCase ->
    fixSentences(courtCaseToSentences(courtCase), username)
  }.toMutableSet()

  fun fixSentencesBySentenceUuids(sentenceUuids: List<UUID>): MutableSet<EventMetadata> = fixSentences(
    sentenceRepository.findBySentenceUuidIn(sentenceUuids)
      .filter { it.statusId != SentenceEntityStatus.DELETED && it.charge.appearanceCharges.firstOrNull { it.appearance?.statusId != CourtAppearanceEntityStatus.DELETED }?.appearance?.courtCase != null }
      .map { sentenceEntity ->
        val charge = sentenceEntity.charge
        val courtAppearance = charge.appearanceCharges.first { it.appearance?.statusId != CourtAppearanceEntityStatus.DELETED }.appearance!!
        val courtCase = courtAppearance.courtCase
        FixSentenceHierarchy(courtCase, courtAppearance.appearanceUuid.toString(), charge.chargeUuid.toString(), sentenceEntity)
      },
  )

  fun fixSentences(sentences: List<FixSentenceHierarchy>, username: String? = null): MutableSet<EventMetadata> {
    val manyChargedSentences = sentences.filter { it.sentenceEntity.statusId == SentenceEntityStatus.MANY_CHARGES_DATA_FIX }
    val toFixSentences = manyChargedSentences
      .groupByTo(mutableMapOf()) { it.sentenceEntity.sentenceUuid }
    val eventsToEmit = mutableSetOf<EventMetadata>()
    sentences.minByOrNull { it.sentenceEntity.id }?.let { minIdSentence ->
      sentenceRepository.acquireSentenceTransactionLock(minIdSentence.sentenceEntity.sentenceUuid)
    }

    toFixSentences.forEach { (originalSentenceUuid, sentenceRecords) ->
      val firstSentenceRecordEventMetadata = sentenceRecords.removeFirst()
      val firstSentenceEventToEmit =
        fixSentence(firstSentenceRecordEventMetadata, EventType.SENTENCE_UPDATED, originalSentenceUuid, username)
      eventsToEmit.add(firstSentenceEventToEmit)
      fixPeriodLengths(firstSentenceRecordEventMetadata, username)

      sentenceRecords.forEach { fixSentenceHierarchy ->
        val sentenceEventToEmit = fixSentence(
          fixSentenceHierarchy,
          EventType.SENTENCE_FIX_SINGLE_CHARGE_INSERTED,
          originalSentenceUuid,
          username,
        ) {
          it.sentenceUuid = UUID.randomUUID()
          it.legacyData?.nomisLineReference = null
        }
        eventsToEmit.add(sentenceEventToEmit)
        val periodLengthEventsToEmit = fixPeriodLengths(fixSentenceHierarchy, username) {
          if (it.periodLengthType == PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS) {
            it.statusId = PeriodLengthEntityStatus.DELETED
          } else {
            it.periodLengthUuid = UUID.randomUUID()
            it.appearanceEntity = null
          }
        }
        eventsToEmit.addAll(periodLengthEventsToEmit)
      }
    }

    val (consecutiveToSentences, nonConsecutiveToSentences) = manyChargedSentences.partition { it.sentenceEntity.consecutiveTo != null }
    markCourtCaseWithUnconfirmedConsecutiveToFix(consecutiveToSentences, username)
    consecutiveToSentences.map { it.courtCase }.distinctBy { it.id }.forEach { consecutiveToCourtCases ->
      if (consecutiveToCourtCases.hasUnconfirmedConsecutiveToFix != true) {
        consecutiveToCourtCases.hasUnconfirmedConsecutiveToFix = true
        consecutiveToCourtCases.updatedAt = ZonedDateTime.now()
        consecutiveToCourtCases.updatedBy = username
        courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(consecutiveToCourtCases, ChangeSource.DPS))
      }
    }
    val checkForConsecutiveToLink = nonConsecutiveToSentences.filter { nonConsecutiveToSentence -> consecutiveToSentences.none { it.courtCase.id == nonConsecutiveToSentence.courtCase.id } }.map { it.sentenceEntity.id }
    val sentencesWithConsecutiveToLink = sentenceRepository.findSentencesWithConsecutiveToLinkAndStatusNot(checkForConsecutiveToLink)
    markCourtCaseWithUnconfirmedConsecutiveToFix(nonConsecutiveToSentences.filter { sentencesWithConsecutiveToLink.contains(it.sentenceEntity.id) }, username)
    return eventsToEmit
  }

  private fun markCourtCaseWithUnconfirmedConsecutiveToFix(fixSentenceHierarchies: List<FixSentenceHierarchy>, username: String?) {
    fixSentenceHierarchies.map { it.courtCase }.distinctBy { it.id }.forEach { courtCase ->
      if (courtCase.hasUnconfirmedConsecutiveToFix != true) {
        courtCase.hasUnconfirmedConsecutiveToFix = true
        courtCase.updatedAt = ZonedDateTime.now()
        courtCase.updatedBy = username
        courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(courtCase, ChangeSource.DPS))
      }
    }
  }

  private fun fixSentence(
    fixSentenceHierarchy: FixSentenceHierarchy,
    sentenceEventType: EventType,
    originalSentenceUuid: UUID,
    username: String? = null,
    sentenceModifyFunction: (SentenceEntity) -> Unit = {},
  ): EventMetadata {
    val sentenceRecord = fixSentenceHierarchy.sentenceEntity
    sentenceRecord.statusId = if (sentenceRecord.legacyData?.active == false) SentenceEntityStatus.INACTIVE else SentenceEntityStatus.ACTIVE
    sentenceRecord.updatedAt = ZonedDateTime.now()
    sentenceRecord.updatedBy = username ?: serviceUserService.getUsername()
    sentenceModifyFunction(sentenceRecord)
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentenceRecord, ChangeSource.DPS))
    return EventMetadataCreator.fixSentenceEventMetadata(
      fixSentenceHierarchy.courtCase.prisonerId,
      fixSentenceHierarchy.courtCase.caseUniqueIdentifier,
      fixSentenceHierarchy.chargeId,
      sentenceRecord.sentenceUuid.toString(),
      fixSentenceHierarchy.courtAppearanceId,
      sentenceEventType,
      originalSentenceUuid.toString(),
    )
  }

  private fun fixPeriodLengths(
    fixSentenceHierarchy: FixSentenceHierarchy,
    username: String? = null,
    periodLengthModifyFunction: (PeriodLengthEntity) -> Unit = {},
  ): MutableSet<EventMetadata> {
    val sentenceRecord = fixSentenceHierarchy.sentenceEntity
    val periodLengthEventsToEmit = mutableSetOf<EventMetadata>()
    val periodLengths = sentenceRecord.periodLengths
    periodLengths.filter { it.statusId == PeriodLengthEntityStatus.MANY_CHARGES_DATA_FIX }
      .forEach { periodLength ->
        periodLength.statusId = PeriodLengthEntityStatus.ACTIVE
        periodLength.updatedAt = ZonedDateTime.now()
        periodLength.updatedBy = username ?: serviceUserService.getUsername()
        // The periodLength.statusId could potentially become DELETED after the following call
        periodLengthModifyFunction(periodLength)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength, ChangeSource.DPS))
        // Do not emit insert events for periodLengths that are DELETED
        if (periodLength.statusId == PeriodLengthEntityStatus.ACTIVE) {
          periodLengthEventsToEmit.add(
            EventMetadataCreator.periodLengthEventMetadata(
              fixSentenceHierarchy.courtCase.prisonerId,
              fixSentenceHierarchy.courtCase.caseUniqueIdentifier,
              fixSentenceHierarchy.courtAppearanceId,
              fixSentenceHierarchy.chargeId,
              sentenceRecord.sentenceUuid.toString(),
              periodLength.periodLengthUuid.toString(),
              EventType.PERIOD_LENGTH_INSERTED,
              false,
            ),
          )
        }
      }
    return periodLengthEventsToEmit
  }
}

data class FixSentenceHierarchy(
  val courtCase: CourtCaseEntity,
  val courtAppearanceId: String,
  val chargeId: String,
  val sentenceEntity: SentenceEntity,
)
