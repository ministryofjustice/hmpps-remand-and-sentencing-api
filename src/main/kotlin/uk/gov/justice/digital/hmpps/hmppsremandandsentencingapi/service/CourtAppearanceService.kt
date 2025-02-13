package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtAppearanceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.util.UUID

@Service
class CourtAppearanceService(
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val periodLengthRepository: PeriodLengthRepository,
  private val chargeService: ChargeService,
  private val serviceUserService: ServiceUserService,
  private val courtCaseRepository: CourtCaseRepository,
  private val documentManagementApiClient: DocumentManagementApiClient,
  private val appearanceTypeRepository: AppearanceTypeRepository,
) {

  @Transactional
  fun createCourtAppearance(createCourtAppearance: CreateCourtAppearance): RecordResponse<CourtAppearanceEntity>? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val courtAppearance = createCourtAppearance(createCourtAppearance, courtCaseEntity)
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + courtAppearance.record)
      return courtAppearance
    }
  }

  @Transactional
  fun createCourtAppearanceByAppearanceUuid(createCourtAppearance: CreateCourtAppearance, appearanceUuid: UUID): RecordResponse<CourtAppearanceEntity>? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val existingCourtAppearance = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)

      val savedAppearance = if (existingCourtAppearance != null) updateCourtAppearanceEntity(createCourtAppearance, courtCaseEntity, existingCourtAppearance) else createCourtAppearanceEntity(createCourtAppearance, courtCaseEntity)
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + savedAppearance.record)
      return savedAppearance
    }
  }

  @Transactional
  fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): RecordResponse<CourtAppearanceEntity> = courtAppearanceRepository.findByAppearanceUuid(courtAppearance.appearanceUuid)?.let { existingCourtAppearance ->
    updateCourtAppearanceEntity(courtAppearance, courtCaseEntity, existingCourtAppearance)
  } ?: createCourtAppearanceEntity(courtAppearance, courtCaseEntity)

  private fun createCourtAppearanceEntity(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): RecordResponse<CourtAppearanceEntity> {
    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    courtAppearance.legacyData = appearanceLegacyData
    val nextCourtAppearance = courtAppearance.nextCourtAppearance?.let { nextCourtAppearance ->
      val futureLegacyData = nextCourtAppearance.appearanceTime?.let { CourtAppearanceLegacyData.from(it) }
      val futureCourtAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.fromFuture(nextCourtAppearance, courtCaseEntity, serviceUserService.getUsername(), courtAppearance.courtCaseReference, futureLegacyData))
      val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nextCourtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type found at ${nextCourtAppearance.appearanceTypeUuid}")
      nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextCourtAppearance, futureCourtAppearance, appearanceType),
      )
    }
    val createdCourtAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername()))
    val eventsToEmit = mutableSetOf(
      EventMetadataCreator.courtAppearanceEventMetadata(
        createdCourtAppearance.courtCase.prisonerId,
        createdCourtAppearance.courtCase.caseUniqueIdentifier,
        createdCourtAppearance.lifetimeUuid.toString(),
        EventType.COURT_APPEARANCE_INSERTED,
      ),
    )
    val chargeRecords = createCharges(courtAppearance.charges, courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, createdCourtAppearance, false)
    createdCourtAppearance.charges.addAll(chargeRecords.map { it.record })
    eventsToEmit.addAll(chargeRecords.flatMap { it.eventsToEmit })
    createdCourtAppearance.nextCourtAppearance = nextCourtAppearance
    courtAppearance.overallSentenceLength?.let { createPeriodLength ->
      val periodLength = PeriodLengthEntity.from(createPeriodLength)
      periodLength.appearanceEntity = createdCourtAppearance
      val createdPeriodLength = periodLengthRepository.save(periodLength)
      createdCourtAppearance.periodLengths = listOf(createdPeriodLength)
    }
    updateDocumentMetadata(createdCourtAppearance, courtCaseEntity.prisonerId)

    nextCourtAppearance?.futureSkeletonAppearance?.also { courtAppearanceEntity ->
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          courtAppearanceEntity.courtCase.prisonerId,
          courtAppearanceEntity.courtCase.caseUniqueIdentifier,
          courtAppearanceEntity.lifetimeUuid.toString(),
          EventType.COURT_APPEARANCE_INSERTED,
        ),
      )
    }
    return RecordResponse(createdCourtAppearance, eventsToEmit)
  }

  private fun updateCourtAppearanceEntity(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity, existingCourtAppearanceEntity: CourtAppearanceEntity): RecordResponse<CourtAppearanceEntity> {
    if (existingCourtAppearanceEntity.statusId == EntityStatus.EDITED) {
      throw ImmutableCourtAppearanceException("Cannot edit an already edited court appearance")
    }
    var appearanceChangeStatus = EntityChangeStatus.NO_CHANGE

    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    courtAppearance.legacyData = appearanceLegacyData
    val compareAppearance = existingCourtAppearanceEntity.copyFrom(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername())
    var activeRecord = existingCourtAppearanceEntity
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var appearanceDateChanged = !existingCourtAppearanceEntity.appearanceDate.isEqual(compareAppearance.appearanceDate)
    if (!existingCourtAppearanceEntity.isSame(compareAppearance)) {
      existingCourtAppearanceEntity.statusId = EntityStatus.EDITED
      val toCreatePeriodLengths = compareAppearance.periodLengths.toList()
      compareAppearance.periodLengths = emptyList()
      activeRecord = courtAppearanceRepository.save(compareAppearance)
      toCreatePeriodLengths.forEach { periodLengthEntity ->
        periodLengthEntity.appearanceEntity = activeRecord
        periodLengthRepository.save(periodLengthEntity)
      }
      appearanceChangeStatus = EntityChangeStatus.EDITED
    }
    val (chargesChangedStatus, chargeEventsToEmit) = updateCharges(courtAppearance.charges, courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, activeRecord, appearanceDateChanged)
    eventsToEmit.addAll(chargeEventsToEmit)
    val (nextCourtAppearanceEntityChangeStatus, futureSkeletonAppearance) = updateNextCourtAppearance(courtAppearance, activeRecord, existingCourtAppearanceEntity.nextCourtAppearance)
    updateDocumentMetadata(activeRecord, courtCaseEntity.prisonerId)
    if (appearanceChangeStatus == EntityChangeStatus.EDITED || chargesChangedStatus == EntityChangeStatus.EDITED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          activeRecord.courtCase.prisonerId,
          activeRecord.courtCase.caseUniqueIdentifier,
          activeRecord.lifetimeUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    }

    if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.CREATED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureSkeletonAppearance!!.courtCase.prisonerId,
          futureSkeletonAppearance.courtCase.caseUniqueIdentifier,
          futureSkeletonAppearance.lifetimeUuid.toString(),
          EventType.COURT_APPEARANCE_INSERTED,
        ),
      )
    } else if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.EDITED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureSkeletonAppearance!!.courtCase.prisonerId,
          futureSkeletonAppearance.courtCase.caseUniqueIdentifier,
          futureSkeletonAppearance.lifetimeUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    } else if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureSkeletonAppearance!!.courtCase.prisonerId,
          futureSkeletonAppearance.courtCase.caseUniqueIdentifier,
          futureSkeletonAppearance.lifetimeUuid.toString(),
          EventType.COURT_APPEARANCE_DELETED,
        ),
      )
    }

    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun updateNextCourtAppearance(
    courtAppearance: CreateCourtAppearance,
    activeRecord: CourtAppearanceEntity,
    existingNextCourtAppearance: NextCourtAppearanceEntity?,
  ): Pair<EntityChangeStatus, CourtAppearanceEntity?> {
    return existingNextCourtAppearance?.let { activeNextCourtAppearance ->
      if (courtAppearance.nextCourtAppearance != null) {
        val activeFutureSkeletonAppearance = activeNextCourtAppearance.futureSkeletonAppearance
        val legacyData = activeFutureSkeletonAppearance.legacyData?.copyFrom(courtAppearance.nextCourtAppearance.appearanceTime)
          ?: courtAppearance.nextCourtAppearance.appearanceTime?.let { CourtAppearanceLegacyData.from(it) }
        var futureCourtAppearance = activeFutureSkeletonAppearance.copyFromFuture(
          courtAppearance.nextCourtAppearance,
          activeRecord.courtCase,
          serviceUserService.getUsername(),
          courtAppearance.courtCaseReference,
          legacyData,
        )
        val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(courtAppearance.nextCourtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type found at ${courtAppearance.nextCourtAppearance.appearanceTypeUuid}")
        val nextCourtAppearance =
          NextCourtAppearanceEntity.from(courtAppearance.nextCourtAppearance, futureCourtAppearance, appearanceType)
        if (!activeNextCourtAppearance.isSame(nextCourtAppearance)) {
          val savedFutureCourtAppearance = courtAppearanceRepository.save(futureCourtAppearance)
          activeFutureSkeletonAppearance.statusId = EntityStatus.EDITED
          activeRecord.nextCourtAppearance = nextCourtAppearanceRepository.save(
            NextCourtAppearanceEntity.from(
              courtAppearance.nextCourtAppearance,
              savedFutureCourtAppearance,
              appearanceType,
            ),
          )
          return@let EntityChangeStatus.EDITED to savedFutureCourtAppearance
        }
        activeRecord.nextCourtAppearance = existingNextCourtAppearance
        EntityChangeStatus.NO_CHANGE to null
      } else {
        activeNextCourtAppearance.futureSkeletonAppearance.statusId = EntityStatus.DELETED
        activeRecord.nextCourtAppearance = null
        EntityChangeStatus.DELETED to activeNextCourtAppearance.futureSkeletonAppearance
      }
    } ?: courtAppearance.nextCourtAppearance?.let { toCreateNextCourtAppearance ->
      val futureLegacyData = toCreateNextCourtAppearance.appearanceTime?.let { CourtAppearanceLegacyData.from(it) }
      val futureCourtAppearance = courtAppearanceRepository.save(
        CourtAppearanceEntity.fromFuture(
          toCreateNextCourtAppearance,
          activeRecord.courtCase,
          serviceUserService.getUsername(),
          courtAppearance.courtCaseReference,
          futureLegacyData,
        ),
      )
      val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(toCreateNextCourtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type found at ${courtAppearance.nextCourtAppearance.appearanceTypeUuid}")
      val savedNextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(toCreateNextCourtAppearance, futureCourtAppearance, appearanceType),
      )
      activeRecord.nextCourtAppearance = savedNextCourtAppearance
      EntityChangeStatus.CREATED to futureCourtAppearance
    } ?: (EntityChangeStatus.NO_CHANGE to null)
  }

  private fun updateCharges(charges: List<CreateCharge>, prisonerId: String, courtCaseUuid: String, existingCourtAppearanceEntity: CourtAppearanceEntity, courtAppearanceDateChanged: Boolean): Pair<EntityChangeStatus, MutableSet<EventMetadata>> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val toDeleteCharges = existingCourtAppearanceEntity.charges.filter { existingCharge -> charges.none { createCharge -> createCharge.chargeUuid == existingCharge.chargeUuid } }
    toDeleteCharges.forEach { chargeEntity ->
      chargeEntity.courtAppearances.removeIf { it.id == existingCourtAppearanceEntity.id }
      eventsToEmit.addAll(chargeService.deleteChargeIfOrphan(chargeEntity, prisonerId, courtCaseUuid).eventsToEmit)
    }
    existingCourtAppearanceEntity.charges.removeAll(toDeleteCharges)
    val chargeRecords = createCharges(charges, prisonerId, courtCaseUuid, existingCourtAppearanceEntity, courtAppearanceDateChanged)
    eventsToEmit.addAll(chargeRecords.flatMap { it.eventsToEmit })
    val createdCharges = chargeRecords.map { it.record }
    val toAddCharges = createdCharges.filter { chargeEntity -> existingCourtAppearanceEntity.charges.none { existingCharge -> chargeEntity.chargeUuid == existingCharge.chargeUuid } }
    existingCourtAppearanceEntity.charges.addAll(toAddCharges)

    return (if (toAddCharges.isNotEmpty() || toDeleteCharges.isNotEmpty()) EntityChangeStatus.EDITED else EntityChangeStatus.NO_CHANGE) to eventsToEmit
  }

  private fun createCharges(
    charges: List<CreateCharge>,
    prisonerId: String,
    courtCaseUuid: String,
    courtAppearanceEntity: CourtAppearanceEntity,
    courtAppearanceDateChanged: Boolean,
  ): MutableSet<RecordResponse<ChargeEntity>> {
    val sentencesCreated = mutableMapOf<String, SentenceEntity>()
    return charges.sortedWith(this::chargesByConsecutiveToLast).map {
      val charge = chargeService.createCharge(it, sentencesCreated, prisonerId, courtCaseUuid, courtAppearanceEntity, courtAppearanceDateChanged)
      charge.record.getActiveSentence()?.let { sentence -> sentence.chargeNumber?.let { it1 -> sentencesCreated.put(it1, sentence) } }
      charge
    }.toMutableSet()
  }

  private fun getAppearanceOutcome(courtAppearance: CreateCourtAppearance): Pair<CourtAppearanceLegacyData?, AppearanceOutcomeEntity?> {
    var appearanceLegacyData = courtAppearance.legacyData
    val appearanceOutcome = courtAppearance.outcomeUuid?.let {
      appearanceLegacyData = appearanceLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null)
      appearanceOutcomeRepository.findByOutcomeUuid(it)
    } ?: appearanceLegacyData?.nomisOutcomeCode?.let { appearanceOutcomeRepository.findByNomisCode(it) }
    return appearanceLegacyData to appearanceOutcome
  }

  fun updateDocumentMetadata(courtAppearanceEntity: CourtAppearanceEntity, prisonerId: String) {
    courtAppearanceEntity.takeIf { it.warrantId != null }?.let {
      documentManagementApiClient.putDocumentMetadata(it.warrantId!!, DocumentMetadata(prisonerId))
    }
  }

  private fun chargesByConsecutiveToLast(first: CreateCharge, second: CreateCharge): Int {
    if (first.sentence?.consecutiveToChargeNumber == null) {
      return -1
    }
    if (first.sentence.consecutiveToChargeNumber == second.sentence?.chargeNumber) {
      return 1
    }
    return 0
  }

  @Transactional
  fun deleteCourtAppearance(courtAppearanceEntity: CourtAppearanceEntity): RecordResponse<CourtAppearanceEntity> {
    courtAppearanceEntity.statusId = EntityStatus.DELETED
    courtAppearanceEntity.charges.filter { it.hasNoActiveCourtAppearances() }.forEach { charge -> chargeService.deleteCharge(charge, courtAppearanceEntity.courtCase.prisonerId, courtAppearanceEntity.courtCase.caseUniqueIdentifier) }
    return RecordResponse(
      courtAppearanceEntity,
      mutableSetOf(
        EventMetadataCreator.courtAppearanceEventMetadata(
          courtAppearanceEntity.courtCase.prisonerId,
          courtAppearanceEntity.courtCase.caseUniqueIdentifier,
          courtAppearanceEntity.lifetimeUuid.toString(),
          EventType.COURT_APPEARANCE_DELETED,
        ),
      ),
    )
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): CourtAppearance? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { CourtAppearance.from(it) }
}
