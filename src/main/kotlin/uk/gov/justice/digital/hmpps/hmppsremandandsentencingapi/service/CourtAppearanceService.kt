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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.util.UUID

@Service
class CourtAppearanceService(
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val periodLengthService: PeriodLengthService,
  private val chargeService: ChargeService,
  private val serviceUserService: ServiceUserService,
  private val courtCaseRepository: CourtCaseRepository,
  private val documentManagementApiClient: DocumentManagementApiClient,
  private val appearanceTypeRepository: AppearanceTypeRepository,
  private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository,
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
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(futureCourtAppearance))
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
        createdCourtAppearance.appearanceUuid.toString(),
        EventType.COURT_APPEARANCE_INSERTED,
      ),
    )
    val chargeRecords = createCharges(courtAppearance.charges, courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, createdCourtAppearance, false)

    chargeRecords.forEach { chargeRecord ->
      createdCourtAppearance.appearanceCharges.add(
        AppearanceChargeEntity(
          courtAppearance = createdCourtAppearance,
          charge = chargeRecord.record,
          createdBy = serviceUserService.getUsername(),
          createdPrison = "TODO", // TODO Replace with actual prison ID or set tu null??
        ),
      )
    }
    eventsToEmit.addAll(chargeRecords.flatMap { it.eventsToEmit })
    createdCourtAppearance.nextCourtAppearance = nextCourtAppearance
    courtAppearance.overallSentenceLength?.let { createPeriodLength ->
      periodLengthService.upsert(listOf(PeriodLengthEntity.from(createPeriodLength, serviceUserService.getUsername())), createdCourtAppearance.periodLengths) { createdPeriodLength ->
        createdPeriodLength.appearanceEntity = createdCourtAppearance
      }
    }
    updateDocumentMetadata(createdCourtAppearance, courtCaseEntity.prisonerId)

    nextCourtAppearance?.futureSkeletonAppearance?.also { courtAppearanceEntity ->
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          courtAppearanceEntity.courtCase.prisonerId,
          courtAppearanceEntity.courtCase.caseUniqueIdentifier,
          courtAppearanceEntity.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_INSERTED,
        ),
      )
    }
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(createdCourtAppearance))
    return RecordResponse(createdCourtAppearance, eventsToEmit)
  }

  private fun updateCourtAppearanceEntity(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity, existingCourtAppearanceEntity: CourtAppearanceEntity): RecordResponse<CourtAppearanceEntity> {
    var appearanceChangeStatus = EntityChangeStatus.NO_CHANGE

    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    courtAppearance.legacyData = appearanceLegacyData
    val compareAppearance = existingCourtAppearanceEntity.copyFrom(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername())
    var activeRecord = existingCourtAppearanceEntity
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var appearanceDateChanged = !existingCourtAppearanceEntity.appearanceDate.isEqual(compareAppearance.appearanceDate)
    if (!existingCourtAppearanceEntity.isSame(compareAppearance)) {
      existingCourtAppearanceEntity.updateFrom(compareAppearance)
      appearanceChangeStatus = EntityChangeStatus.EDITED
    }
    val toCreatePeriodLengths = courtAppearance.overallSentenceLength?.let { listOf(PeriodLengthEntity.from(it, serviceUserService.getUsername())) } ?: emptyList<PeriodLengthEntity>()
    val periodLengthChangeStatus = periodLengthService.upsert(toCreatePeriodLengths, existingCourtAppearanceEntity.periodLengths) { createdPeriodLength ->
      createdPeriodLength.appearanceEntity = existingCourtAppearanceEntity
    }
    val (chargesChangedStatus, chargeEventsToEmit) = updateCharges(courtAppearance.charges, courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, activeRecord, appearanceDateChanged)
    eventsToEmit.addAll(chargeEventsToEmit)
    val (nextCourtAppearanceEntityChangeStatus, futureSkeletonAppearance) = updateNextCourtAppearance(courtAppearance, activeRecord, existingCourtAppearanceEntity.nextCourtAppearance)
    updateDocumentMetadata(activeRecord, courtCaseEntity.prisonerId)
    if (appearanceChangeStatus == EntityChangeStatus.EDITED || chargesChangedStatus == EntityChangeStatus.EDITED || periodLengthChangeStatus != EntityChangeStatus.NO_CHANGE) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          activeRecord.courtCase.prisonerId,
          activeRecord.courtCase.caseUniqueIdentifier,
          activeRecord.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    }

    if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.CREATED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureSkeletonAppearance!!.courtCase.prisonerId,
          futureSkeletonAppearance.courtCase.caseUniqueIdentifier,
          futureSkeletonAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_INSERTED,
        ),
      )
    } else if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.EDITED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureSkeletonAppearance!!.courtCase.prisonerId,
          futureSkeletonAppearance.courtCase.caseUniqueIdentifier,
          futureSkeletonAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    } else if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureSkeletonAppearance!!.courtCase.prisonerId,
          futureSkeletonAppearance.courtCase.caseUniqueIdentifier,
          futureSkeletonAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_DELETED,
        ),
      )
    }
    if (appearanceChangeStatus != EntityChangeStatus.NO_CHANGE || setOf(EntityChangeStatus.CREATED, EntityChangeStatus.DELETED).contains(nextCourtAppearanceEntityChangeStatus)) {
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(existingCourtAppearanceEntity))
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
          activeFutureSkeletonAppearance.updateFrom(futureCourtAppearance)
          courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(activeFutureSkeletonAppearance))
          activeRecord.nextCourtAppearance = nextCourtAppearanceRepository.save(
            NextCourtAppearanceEntity.from(
              courtAppearance.nextCourtAppearance,
              activeFutureSkeletonAppearance,
              appearanceType,
            ),
          )
          return@let EntityChangeStatus.EDITED to activeFutureSkeletonAppearance
        }
        activeRecord.nextCourtAppearance = existingNextCourtAppearance
        EntityChangeStatus.NO_CHANGE to null
      } else {
        activeNextCourtAppearance.futureSkeletonAppearance.delete(serviceUserService.getUsername())
        courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(activeNextCourtAppearance.futureSkeletonAppearance))
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
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(futureCourtAppearance))
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
    val toDeleteCharges = existingCourtAppearanceEntity.appearanceCharges
      .filter { appearanceCharge -> charges.none { createCharge -> createCharge.chargeUuid == appearanceCharge.charge.chargeUuid } }

    toDeleteCharges.forEach { appearanceCharge ->
      eventsToEmit.addAll(
        chargeService.deleteChargeIfOrphan(
          appearanceCharge.charge,
          prisonerId,
          courtCaseUuid,
          existingCourtAppearanceEntity.appearanceUuid.toString(),
        ).eventsToEmit,
      )
    }
    existingCourtAppearanceEntity.appearanceCharges.removeAll(toDeleteCharges)
    val chargeRecords = createCharges(charges, prisonerId, courtCaseUuid, existingCourtAppearanceEntity, courtAppearanceDateChanged)
    eventsToEmit.addAll(chargeRecords.flatMap { it.eventsToEmit })
    val createdCharges = chargeRecords.map { it.record }
    val toAddCharges = createdCharges.filter { chargeEntity ->
      existingCourtAppearanceEntity.appearanceCharges.none { it.charge.chargeUuid == chargeEntity.chargeUuid }
    }
    toAddCharges.forEach { charge ->
      existingCourtAppearanceEntity.appearanceCharges.add(
        AppearanceChargeEntity(
          courtAppearance = existingCourtAppearanceEntity,
          charge = charge,
          createdBy = serviceUserService.getUsername(),
          createdPrison = "TODO", // TODO Replace with actual prison ID or null
        ),
      )
    }

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
    courtAppearanceEntity.delete(serviceUserService.getUsername())
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(courtAppearanceEntity))
    courtAppearanceEntity.appearanceCharges
      .map { it.charge }
      .filter { it.hasNoActiveCourtAppearances() }
      .forEach { charge ->
        chargeService.deleteCharge(
          charge,
          courtAppearanceEntity.courtCase.prisonerId,
          courtAppearanceEntity.courtCase.caseUniqueIdentifier,
          courtAppearanceEntity.appearanceUuid.toString(),
        )
      }

    return RecordResponse(
      courtAppearanceEntity,
      mutableSetOf(
        EventMetadataCreator.courtAppearanceEventMetadata(
          courtAppearanceEntity.courtCase.prisonerId,
          courtAppearanceEntity.courtCase.caseUniqueIdentifier,
          courtAppearanceEntity.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_DELETED,
        ),
      ),
    )
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): CourtAppearance? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { CourtAppearance.from(it) }
}
