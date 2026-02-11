package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteCourtAppearanceResponse
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
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
  private val appearanceTypeRepository: AppearanceTypeRepository,
  private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository,
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService,
  private val documentService: UploadedDocumentService,
) {

  @Transactional
  fun createCourtAppearance(createCourtAppearance: CreateCourtAppearance): RecordResponse<CourtAppearanceEntity>? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)
      ?.let { courtCaseEntity ->
        val courtAppearance = createCourtAppearance(createCourtAppearance, courtCaseEntity)
        courtCaseEntity.latestCourtAppearance =
          CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + courtAppearance.record)
        return courtAppearance
      }
  }

  @Transactional
  fun createCourtAppearanceByAppearanceUuid(
    createCourtAppearance: CreateCourtAppearance,
    appearanceUuid: UUID,
  ): RecordResponse<CourtAppearanceEntity>? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)
      ?.let { courtCaseEntity ->
        val existingCourtAppearance = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)

        val savedAppearance = if (existingCourtAppearance != null) {
          updateCourtAppearanceEntity(
            createCourtAppearance,
            courtCaseEntity,
            existingCourtAppearance,
          )
        } else {
          createCourtAppearanceEntity(createCourtAppearance, courtCaseEntity)
        }
        courtCaseEntity.latestCourtAppearance =
          CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + savedAppearance.record)
        return savedAppearance
      }
  }

  @Transactional
  fun createCourtAppearance(
    courtAppearance: CreateCourtAppearance,
    courtCaseEntity: CourtCaseEntity,
  ): RecordResponse<CourtAppearanceEntity> = courtAppearanceRepository.findByAppearanceUuid(courtAppearance.appearanceUuid)?.let { existingCourtAppearance ->
    updateCourtAppearanceEntity(courtAppearance, courtCaseEntity, existingCourtAppearance)
  } ?: createCourtAppearanceEntity(courtAppearance, courtCaseEntity)

  private fun createCourtAppearanceEntity(
    courtAppearance: CreateCourtAppearance,
    courtCaseEntity: CourtCaseEntity,
  ): RecordResponse<CourtAppearanceEntity> {
    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    courtAppearance.legacyData = appearanceLegacyData
    val nextCourtAppearance = courtAppearance.nextCourtAppearance?.let { nextCourtAppearance ->
      val futureLegacyData = nextCourtAppearance.appearanceTime?.let { CourtAppearanceLegacyData.from(it) }
      val futureCourtAppearance = courtAppearanceRepository.save(
        CourtAppearanceEntity.fromFuture(
          nextCourtAppearance,
          courtCaseEntity,
          serviceUserService.getUsername(),
          courtAppearance.courtCaseReference,
          futureLegacyData,
        ),
      )
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(futureCourtAppearance, ChangeSource.DPS))
      val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nextCourtAppearance.appearanceTypeUuid)
        ?: throw EntityNotFoundException("No appearance type found at ${nextCourtAppearance.appearanceTypeUuid}")
      nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextCourtAppearance, futureCourtAppearance, appearanceType),
      )
    }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(
        courtAppearance,
        appearanceOutcome,
        courtCaseEntity,
        serviceUserService.getUsername(),
      ),
    )

    val eventsToEmit = mutableSetOf(
      EventMetadataCreator.courtAppearanceEventMetadata(
        createdCourtAppearance.courtCase.prisonerId,
        createdCourtAppearance.courtCase.caseUniqueIdentifier,
        createdCourtAppearance.appearanceUuid.toString(),
        EventType.COURT_APPEARANCE_INSERTED,
      ),
    )
    val chargeRecords = createCharges(
      courtAppearance.charges,
      courtCaseEntity.prisonerId,
      courtCaseEntity.caseUniqueIdentifier,
      createdCourtAppearance,
      false,
      nextCourtAppearance?.futureSkeletonAppearance,
    )

    chargeRecords.heardCharges.forEach { chargeRecord ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        createdCourtAppearance,
        chargeRecord.record,
        serviceUserService.getUsername(),
        createdCourtAppearance.createdPrison,
      )
      createdCourtAppearance.appearanceCharges.add(appearanceChargeEntity)
      chargeRecord.record.appearanceCharges.add(appearanceChargeEntity)
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity, ChangeSource.DPS))
    }
    nextCourtAppearance?.futureSkeletonAppearance?.let { futureCourtAppearance ->
      chargeRecords.futureCharges.forEach { chargeRecord ->
        val appearanceChargeEntity = AppearanceChargeEntity(
          futureCourtAppearance,
          chargeRecord.record,
          serviceUserService.getUsername(),
          createdCourtAppearance.createdPrison,
        )
        futureCourtAppearance.appearanceCharges.add(appearanceChargeEntity)
        chargeRecord.record.appearanceCharges.add(appearanceChargeEntity)
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity, ChangeSource.DPS))
      }
    }
    eventsToEmit.addAll(chargeRecords.heardCharges.flatMap { it.eventsToEmit } + chargeRecords.futureCharges.flatMap { it.eventsToEmit })
    createdCourtAppearance.nextCourtAppearance = nextCourtAppearance
    courtAppearance.overallSentenceLength?.let { createPeriodLength ->
      // Ignore period-length events returned here because we do not emit them from createCourtAppearanceEntity
      periodLengthService.create(
        listOf(PeriodLengthEntity.from(createPeriodLength, serviceUserService.getUsername())),
        createdCourtAppearance.periodLengths,
        courtCaseEntity.prisonerId,
        { createdPeriodLength ->
          createdPeriodLength.appearanceEntity = createdCourtAppearance
        },
      )
    }

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

    documentService.update(
      courtAppearance.documents?.map { it.documentUUID } ?: emptyList(),
      createdCourtAppearance,
    )

    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(createdCourtAppearance, ChangeSource.DPS))
    return RecordResponse(createdCourtAppearance, eventsToEmit)
  }

  private fun updateCourtAppearanceEntity(
    courtAppearance: CreateCourtAppearance,
    courtCaseEntity: CourtCaseEntity,
    existingCourtAppearanceEntity: CourtAppearanceEntity,
  ): RecordResponse<CourtAppearanceEntity> {
    var appearanceChangeStatus = EntityChangeStatus.NO_CHANGE

    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    courtAppearance.legacyData = appearanceLegacyData
    val compareAppearance = existingCourtAppearanceEntity.copyFrom(
      courtAppearance,
      appearanceOutcome,
      courtCaseEntity,
      serviceUserService.getUsername(),
    )
    val activeRecord = existingCourtAppearanceEntity
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val appearanceDateChanged = !existingCourtAppearanceEntity.appearanceDate.isEqual(compareAppearance.appearanceDate)
    if (!existingCourtAppearanceEntity.isSame(compareAppearance)) {
      existingCourtAppearanceEntity.updateFrom(compareAppearance)
      appearanceChangeStatus = EntityChangeStatus.EDITED
    }
    val toCreatePeriodLengths = courtAppearance.overallSentenceLength?.let {
      listOf(
        PeriodLengthEntity.from(
          it,
          serviceUserService.getUsername(),
        ),
      )
    } ?: emptyList()
    // Ignore period-length events returned here because we do not emit them from updateCourtAppearanceEntity
    periodLengthService.delete(
      toCreatePeriodLengths,
      existingCourtAppearanceEntity.periodLengths,
      courtCaseEntity.prisonerId,
      existingCourtAppearanceEntity.appearanceUuid.toString(),
      courtCaseEntity.caseUniqueIdentifier,
    )

    periodLengthService.update(
      toCreatePeriodLengths,
      existingCourtAppearanceEntity.periodLengths,
      courtCaseEntity.prisonerId,
    )

    periodLengthService.create(
      toCreatePeriodLengths,
      existingCourtAppearanceEntity.periodLengths,
      courtCaseEntity.prisonerId,
      { created -> created.appearanceEntity = existingCourtAppearanceEntity },
    )

    val (chargesChangedStatus, chargeEventsToEmit) = updateCharges(
      courtAppearance.charges,
      courtCaseEntity.prisonerId,
      courtCaseEntity.caseUniqueIdentifier,
      activeRecord,
      appearanceDateChanged,
      courtAppearance.prisonId,
    )
    eventsToEmit.addAll(chargeEventsToEmit)
    val nextCourtAppearanceRecord = updateNextCourtAppearance(
      courtAppearance,
      activeRecord,
      existingCourtAppearanceEntity.nextCourtAppearance,
    )
    if (appearanceChangeStatus == EntityChangeStatus.EDITED || chargesChangedStatus == EntityChangeStatus.EDITED) {
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          activeRecord.courtCase.prisonerId,
          activeRecord.courtCase.caseUniqueIdentifier,
          activeRecord.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    }
    eventsToEmit.addAll(nextCourtAppearanceRecord?.second?.eventsToEmit ?: mutableSetOf())

    documentService.update(
      courtAppearance.documents?.map { it.documentUUID } ?: emptyList(),
      activeRecord,
    )

    if (appearanceChangeStatus != EntityChangeStatus.NO_CHANGE ||
      setOf(
        EntityChangeStatus.CREATED,
        EntityChangeStatus.DELETED,
      ).contains(nextCourtAppearanceRecord?.first)
    ) {
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(existingCourtAppearanceEntity, ChangeSource.DPS))
    }
    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun updateNextCourtAppearance(
    courtAppearance: CreateCourtAppearance,
    activeRecord: CourtAppearanceEntity,
    existingNextCourtAppearance: NextCourtAppearanceEntity?,
  ): Pair<EntityChangeStatus, RecordResponse<CourtAppearanceEntity>>? {
    return existingNextCourtAppearance?.let { activeNextCourtAppearance ->
      if (courtAppearance.nextCourtAppearance != null) {
        val activeFutureSkeletonAppearance = activeNextCourtAppearance.futureSkeletonAppearance
        val legacyData =
          activeFutureSkeletonAppearance.legacyData?.copyFrom(courtAppearance.nextCourtAppearance.appearanceTime)
            ?: courtAppearance.nextCourtAppearance.appearanceTime?.let { CourtAppearanceLegacyData.from(it) }
        val futureCourtAppearance = activeFutureSkeletonAppearance.copyFromFuture(
          courtAppearance.nextCourtAppearance,
          activeRecord.courtCase,
          serviceUserService.getUsername(),
          courtAppearance.courtCaseReference,
          legacyData,
        )
        val appearanceType =
          appearanceTypeRepository.findByAppearanceTypeUuid(courtAppearance.nextCourtAppearance.appearanceTypeUuid)
            ?: throw EntityNotFoundException("No appearance type found at ${courtAppearance.nextCourtAppearance.appearanceTypeUuid}")
        val nextCourtAppearance =
          NextCourtAppearanceEntity.from(courtAppearance.nextCourtAppearance, futureCourtAppearance, appearanceType)
        if (!activeNextCourtAppearance.isSame(nextCourtAppearance)) {
          activeFutureSkeletonAppearance.updateFrom(futureCourtAppearance)
          courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(activeFutureSkeletonAppearance, ChangeSource.DPS))
          nextCourtAppearance.futureSkeletonAppearance = activeFutureSkeletonAppearance
          activeNextCourtAppearance.updateFrom(nextCourtAppearance)
          val eventsToEmit = mutableSetOf(
            EventMetadataCreator.courtAppearanceEventMetadata(
              activeFutureSkeletonAppearance.courtCase.prisonerId,
              activeFutureSkeletonAppearance.courtCase.caseUniqueIdentifier,
              activeFutureSkeletonAppearance.appearanceUuid.toString(),
              EventType.COURT_APPEARANCE_UPDATED,
            ),
          )
          return@let EntityChangeStatus.EDITED to RecordResponse(activeFutureSkeletonAppearance, eventsToEmit)
        }
        activeRecord.nextCourtAppearance = existingNextCourtAppearance
        EntityChangeStatus.NO_CHANGE to RecordResponse(existingNextCourtAppearance.futureSkeletonAppearance, mutableSetOf())
      } else {
        val eventsToEmit = mutableSetOf<EventMetadata>()
        var changeStatus = EntityChangeStatus.NO_CHANGE
        if (activeNextCourtAppearance.futureSkeletonAppearance.statusId == CourtAppearanceEntityStatus.FUTURE) {
          val deletedAppearanceRecord = deleteCourtAppearance(activeNextCourtAppearance.futureSkeletonAppearance)
          eventsToEmit.addAll(deletedAppearanceRecord.eventsToEmit)
          changeStatus = EntityChangeStatus.DELETED
        }
        activeRecord.nextCourtAppearance = null
        changeStatus to RecordResponse(activeNextCourtAppearance.futureSkeletonAppearance, eventsToEmit)
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
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(futureCourtAppearance, ChangeSource.DPS))
      val appearanceType =
        appearanceTypeRepository.findByAppearanceTypeUuid(toCreateNextCourtAppearance.appearanceTypeUuid)
          ?: throw EntityNotFoundException("No appearance type found at ${courtAppearance.nextCourtAppearance.appearanceTypeUuid}")
      val savedNextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(toCreateNextCourtAppearance, futureCourtAppearance, appearanceType),
      )
      val eventsToEmit = activeRecord.appearanceCharges.filter { it.charge?.isInterim() == true }
        .flatMap {
          val futureChargeRecord = chargeService.createFutureDatedCharge(
            it.charge!!,
            activeRecord.courtCase.prisonerId,
            activeRecord.courtCase.caseUniqueIdentifier,
            futureCourtAppearance,
          )
          val appearanceCharge = AppearanceChargeEntity(
            futureCourtAppearance,
            futureChargeRecord.record,
            serviceUserService.getUsername(),
            courtAppearance.prisonId,
          )
          futureCourtAppearance.appearanceCharges.add(appearanceCharge)
          futureChargeRecord.record.appearanceCharges.add(appearanceCharge)
          appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge, ChangeSource.DPS))
          futureChargeRecord.eventsToEmit
        }.toMutableSet()
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          futureCourtAppearance.courtCase.prisonerId,
          futureCourtAppearance.courtCase.caseUniqueIdentifier,
          futureCourtAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_INSERTED,
        ),
      )
      activeRecord.nextCourtAppearance = savedNextCourtAppearance

      EntityChangeStatus.CREATED to RecordResponse(futureCourtAppearance, eventsToEmit)
    }
  }

  private fun updateCharges(
    charges: List<CreateCharge>,
    prisonerId: String,
    courtCaseUuid: String,
    existingCourtAppearanceEntity: CourtAppearanceEntity,
    courtAppearanceDateChanged: Boolean,
    prisonIdForUpdate: String,
  ): Pair<EntityChangeStatus, MutableSet<EventMetadata>> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val toDeleteCharges = existingCourtAppearanceEntity.appearanceCharges
      .map { it.charge!! }
      .filter { charge -> charges.none { createCharge -> createCharge.chargeUuid == charge.chargeUuid } }

    val toDeleteLinks = existingCourtAppearanceEntity.appearanceCharges.filter { toDeleteCharges.contains(it.charge!!) }

    toDeleteLinks.forEach { appearanceCharge ->
      appearanceChargeHistoryRepository.save(
        AppearanceChargeHistoryEntity.removedFrom(
          appearanceCharge = appearanceCharge,
          removedBy = serviceUserService.getUsername(),
          removedPrison = prisonIdForUpdate,
          ChangeSource.DPS,
        ),
      )
      appearanceCharge.appearance!!.appearanceCharges.remove(appearanceCharge)
      appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
      appearanceCharge.charge = null
      appearanceCharge.appearance = null
    }

    toDeleteCharges.forEach { charge ->
      eventsToEmit.addAll(
        chargeService.deleteChargeIfOrphan(
          charge,
          prisonerId,
          courtCaseUuid,
          existingCourtAppearanceEntity.appearanceUuid.toString(),
        ).eventsToEmit,
      )
    }

    val (heardCharges) =
      createCharges(charges, prisonerId, courtCaseUuid, existingCourtAppearanceEntity, courtAppearanceDateChanged, null)
    eventsToEmit.addAll(heardCharges.flatMap { it.eventsToEmit })
    val createdCharges = heardCharges.map { it.record }
    val toAddCharges = createdCharges.filter { chargeEntity ->
      existingCourtAppearanceEntity.appearanceCharges.none { it.charge!!.chargeUuid == chargeEntity.chargeUuid }
    }
    toAddCharges.forEach { charge ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        existingCourtAppearanceEntity,
        charge,
        serviceUserService.getUsername(),
        prisonIdForUpdate,
      )
      existingCourtAppearanceEntity.appearanceCharges.add(appearanceChargeEntity)
      charge.appearanceCharges.add(appearanceChargeEntity)
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity, ChangeSource.DPS))
    }

    return (if (toAddCharges.isNotEmpty() || toDeleteCharges.isNotEmpty()) EntityChangeStatus.EDITED else EntityChangeStatus.NO_CHANGE) to eventsToEmit
  }

  @VisibleForTesting
  internal fun createCharges(
    charges: List<CreateCharge>,
    prisonerId: String,
    courtCaseUuid: String,
    courtAppearanceEntity: CourtAppearanceEntity,
    courtAppearanceDateChanged: Boolean,
    futureCourtAppearanceEntity: CourtAppearanceEntity?,
  ): ChargesCreated {
    val sentencesCreated = mutableMapOf<UUID, SentenceEntity>()
    val allChargeRecords = mutableSetOf<RecordResponse<ChargeEntity>>()

    val (replacedCharges, otherCharges) = charges.partition { it.outcomeUuid == ChargeService.replacedWithAnotherOutcomeUuid }

    val createdReplacedCharges = replacedCharges.map { charge ->
      chargeService.createCharge(
        charge,
        sentencesCreated,
        prisonerId,
        courtCaseUuid,
        courtAppearanceEntity,
        courtAppearanceDateChanged,
      )
    }
    allChargeRecords.addAll(createdReplacedCharges)
    val createdReplacedChargeMap = createdReplacedCharges.associate { it.record.chargeUuid to it.record }

    val orderedOtherCharges = orderChargesByConsecutiveChain(otherCharges)

    val createdOtherCharges = orderedOtherCharges.map { charge ->
      val supersedingCharge = charge.replacingChargeUuid?.let { createdReplacedChargeMap[it] }
      chargeService.createCharge(
        charge,
        sentencesCreated,
        prisonerId,
        courtCaseUuid,
        courtAppearanceEntity,
        courtAppearanceDateChanged,
        supersedingCharge,
      )
    }
    allChargeRecords.addAll(createdOtherCharges)

    val futureChargeRecords = futureCourtAppearanceEntity?.let { futureCourtAppearance ->
      allChargeRecords.map { it.record }
        .filter { it.isInterim() }
        .map {
          chargeService.createFutureDatedCharge(it, prisonerId, courtCaseUuid, futureCourtAppearance)
        }
    }?.toMutableSet() ?: mutableSetOf()
    return ChargesCreated(allChargeRecords, futureChargeRecords)
  }

  private fun getAppearanceOutcome(courtAppearance: CreateCourtAppearance): Pair<CourtAppearanceLegacyData?, AppearanceOutcomeEntity?> {
    var appearanceLegacyData = courtAppearance.legacyData
    val appearanceOutcome = courtAppearance.outcomeUuid?.let {
      appearanceLegacyData = appearanceLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null)
      appearanceOutcomeRepository.findByOutcomeUuid(it)
    } ?: appearanceLegacyData?.nomisOutcomeCode?.let { appearanceOutcomeRepository.findByNomisCode(it) }
    return appearanceLegacyData to appearanceOutcome
  }

  @VisibleForTesting
  fun orderChargesByConsecutiveChain(charges: List<CreateCharge>): List<CreateCharge> {
    val chargesWithSentences = charges.filter { it.sentence != null }
    val chargesBySentenceUuid: Map<UUID, CreateCharge> =
      chargesWithSentences.associateBy { it.sentence!!.sentenceUuid }

    val chainPositionByRef = mutableMapOf<UUID, Int>()

    val chargesWithSortKeys = charges.map { charge ->
      val sentenceUuid = charge.sentence?.sentenceUuid
      val positionInChain = chainPositionFor(sentenceUuid, chargesBySentenceUuid, chainPositionByRef)
      ChargeWithSortKeys(positionInChain, charge)
    }

    // Sort by positionInChain, sentenceRef
    return chargesWithSortKeys.sortedWith(compareBy { it.chainPosition }).map { it.charge }
  }

  private data class ChargeWithSortKeys(
    val chainPosition: Int,
    val charge: CreateCharge,
  )

  /**
   * Determines "position in chain" of a sentenceUuid, called recursively
   *
   * - If `sentenceUuid` is null, position = 0 (implies charge has no sentence).
   * - If the parent is not present in this `charges` list: treat it as position 0.
   */
  private fun chainPositionFor(
    sentenceUuid: UUID?,
    chargesBySentenceUuid: Map<UUID, CreateCharge>,
    chainPositionByUuid: MutableMap<UUID, Int>,
  ): Int {
    if (sentenceUuid == null) return 0
    // Already processed
    chainPositionByUuid[sentenceUuid]?.let { return it }

    val parentUuid = chargesBySentenceUuid[sentenceUuid]?.sentence?.consecutiveToSentenceUuid
    val parentPosition = if (parentUuid != null && chargesBySentenceUuid.containsKey(parentUuid)) {
      chainPositionFor(parentUuid, chargesBySentenceUuid, chainPositionByUuid)
    } else {
      0 // No parent in the chain
    }
    val chainPosition = parentPosition + 1

    chainPositionByUuid[sentenceUuid] = chainPosition

    return chainPosition
  }

  @Transactional
  fun deleteCourtAppearance(courtAppearanceEntity: CourtAppearanceEntity): RecordResponse<CourtAppearanceEntity> {
    courtAppearanceEntity.delete(serviceUserService.getUsername())
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(courtAppearanceEntity, ChangeSource.DPS))
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    eventsToEmit.add(
      EventMetadataCreator.courtAppearanceEventMetadata(
        courtAppearanceEntity.courtCase.prisonerId,
        courtAppearanceEntity.courtCase.caseUniqueIdentifier,
        courtAppearanceEntity.appearanceUuid.toString(),
        EventType.COURT_APPEARANCE_DELETED,
      ),
    )
    courtAppearanceEntity.appearanceCharges
      .removeAll { appearanceCharge ->
        if (appearanceCharge.charge!!.hasNoLiveCourtAppearances()) {
          eventsToEmit.addAll(
            chargeService.deleteCharge(
              appearanceCharge.charge!!,
              courtAppearanceEntity.courtCase.prisonerId,
              courtAppearanceEntity.courtCase.caseUniqueIdentifier,
              courtAppearanceEntity.appearanceUuid.toString(),
            ).eventsToEmit,
          )
        }
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.removedFrom(appearanceCharge, serviceUserService.getUsername(), null, ChangeSource.DPS))
        appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
        appearanceCharge.appearance = null
        appearanceCharge.charge = null
        true
      }
    return RecordResponse(
      courtAppearanceEntity,
      eventsToEmit,
    )
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): RecordResponse<CourtAppearance>? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.takeUnless { it.statusId == CourtAppearanceEntityStatus.DELETED }?.let {
    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(listOf(it.courtCase))
    RecordResponse(CourtAppearance.from(it), eventsToEmit)
  }

  @Transactional
  fun delete(courtAppearanceUUID: UUID): DeleteCourtAppearanceResponse {
    val courtAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(courtAppearanceUUID)
      ?: throw EntityNotFoundException("No court appearance found at $courtAppearanceUUID")
    val courtCaseEntity = courtAppearanceEntity.courtCase

    val eventsToEmit = deleteCourtAppearance(courtAppearanceEntity).eventsToEmit.toMutableSet()
    courtAppearanceEntity.documents.forEach { document ->
      document.unlink(
        username = serviceUserService.getUsername(),
      )
    }

    courtAppearanceEntity.nextCourtAppearance?.futureSkeletonAppearance?.takeUnless { it.statusId == CourtAppearanceEntityStatus.ACTIVE }?.let { futureAppearance ->
      eventsToEmit.addAll(deleteCourtAppearance(futureAppearance).eventsToEmit)
    }

    if (courtCaseEntity.appearances.all { it.statusId == CourtAppearanceEntityStatus.DELETED }) {
      courtCaseEntity.latestCourtAppearance = null
      courtCaseEntity.delete(serviceUserService.getUsername())
      return DeleteCourtAppearanceResponse(
        records = RecordResponse(
          courtAppearanceEntity,
          (
            eventsToEmit + mutableSetOf(
              EventMetadataCreator.courtCaseEventMetadata(
                courtCaseEntity.prisonerId,
                courtCaseEntity.caseUniqueIdentifier,
                EventType.COURT_CASE_DELETED,
              ),
            )
            ) as MutableSet<EventMetadata>,
        ),
        courtCaseUuid = courtCaseEntity.caseUniqueIdentifier,
      )
    }
    courtCaseEntity.latestCourtAppearance =
      CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances - courtAppearanceEntity)

    return DeleteCourtAppearanceResponse(
      records = RecordResponse(
        courtAppearanceEntity,
        (
          eventsToEmit + mutableSetOf(
            EventMetadataCreator.courtCaseEventMetadata(
              courtCaseEntity.prisonerId,
              courtCaseEntity.caseUniqueIdentifier,
              EventType.COURT_CASE_UPDATED,
            ),
          )
          ) as MutableSet<EventMetadata>,
      ),
      courtCaseUuid = courtCaseEntity.caseUniqueIdentifier,
    )
  }
}

data class ChargesCreated(
  val heardCharges: MutableSet<RecordResponse<ChargeEntity>>,
  val futureCharges: MutableSet<RecordResponse<ChargeEntity>>,
)
