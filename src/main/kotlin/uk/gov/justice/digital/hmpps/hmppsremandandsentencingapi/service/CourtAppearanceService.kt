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
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(futureCourtAppearance))
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
    )

    chargeRecords.forEach { chargeRecord ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        createdCourtAppearance,
        chargeRecord.record,
        serviceUserService.getUsername(),
        createdCourtAppearance.createdPrison,
      )
      createdCourtAppearance.appearanceCharges.add(appearanceChargeEntity)
      chargeRecord.record.appearanceCharges.add(appearanceChargeEntity)
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity))
    }
    eventsToEmit.addAll(chargeRecords.flatMap { it.eventsToEmit })
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

    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(createdCourtAppearance))
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
    var activeRecord = existingCourtAppearanceEntity
    val eventsToEmit = mutableSetOf<EventMetadata>()
    var appearanceDateChanged = !existingCourtAppearanceEntity.appearanceDate.isEqual(compareAppearance.appearanceDate)
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
    } ?: emptyList<PeriodLengthEntity>()
    // Ignore period-length events returned here because we do not emit them from updateCourtAppearanceEntity
    periodLengthService.delete(
      toCreatePeriodLengths,
      existingCourtAppearanceEntity.periodLengths,
      courtCaseEntity.prisonerId,
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
    val (nextCourtAppearanceEntityChangeStatus, futureSkeletonAppearance) = updateNextCourtAppearance(
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

    documentService.update(
      courtAppearance.documents?.map { it.documentUUID } ?: emptyList(),
      activeRecord,
    )

    if (appearanceChangeStatus != EntityChangeStatus.NO_CHANGE ||
      setOf(
        EntityChangeStatus.CREATED,
        EntityChangeStatus.DELETED,
      ).contains(nextCourtAppearanceEntityChangeStatus)
    ) {
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
          courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(activeFutureSkeletonAppearance))
          nextCourtAppearance.futureSkeletonAppearance = activeFutureSkeletonAppearance
          activeNextCourtAppearance.updateFrom(nextCourtAppearance)
          return@let EntityChangeStatus.EDITED to activeFutureSkeletonAppearance
        }
        activeRecord.nextCourtAppearance = existingNextCourtAppearance
        EntityChangeStatus.NO_CHANGE to null
      } else {
        var futureSkeletonChangeStatus: Pair<EntityChangeStatus, CourtAppearanceEntity?> = EntityChangeStatus.NO_CHANGE to null
        if (activeNextCourtAppearance.futureSkeletonAppearance.statusId == CourtAppearanceEntityStatus.FUTURE) {
          activeNextCourtAppearance.futureSkeletonAppearance.delete(serviceUserService.getUsername())
          courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(activeNextCourtAppearance.futureSkeletonAppearance))
          futureSkeletonChangeStatus = EntityChangeStatus.DELETED to activeNextCourtAppearance.futureSkeletonAppearance
        }
        activeRecord.nextCourtAppearance = null
        futureSkeletonChangeStatus
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
      val appearanceType =
        appearanceTypeRepository.findByAppearanceTypeUuid(toCreateNextCourtAppearance.appearanceTypeUuid)
          ?: throw EntityNotFoundException("No appearance type found at ${courtAppearance.nextCourtAppearance.appearanceTypeUuid}")
      val savedNextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(toCreateNextCourtAppearance, futureCourtAppearance, appearanceType),
      )
      activeRecord.nextCourtAppearance = savedNextCourtAppearance
      EntityChangeStatus.CREATED to futureCourtAppearance
    } ?: (EntityChangeStatus.NO_CHANGE to null)
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
        ),
      )
      appearanceCharge.appearance!!.appearanceCharges.remove(appearanceCharge)
      appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
      appearanceCharge.charge = null
      appearanceCharge.appearance = null
    }

    val removedCharges = mutableSetOf<AppearanceChargeEntity>()
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

    val chargeRecords =
      createCharges(charges, prisonerId, courtCaseUuid, existingCourtAppearanceEntity, courtAppearanceDateChanged)
    eventsToEmit.addAll(chargeRecords.flatMap { it.eventsToEmit })
    val createdCharges = chargeRecords.map { it.record }
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
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity))
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
    val sentencesCreated = mutableMapOf<UUID, SentenceEntity>()
    return orderChargesByConsecutiveChain(charges).map {
      val charge = chargeService.createCharge(
        it,
        sentencesCreated,
        prisonerId,
        courtCaseUuid,
        courtAppearanceEntity,
        courtAppearanceDateChanged,
      )
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
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(courtAppearanceEntity))
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
      .forEach { appearanceCharge ->
        if (appearanceCharge.charge!!.hasNoActiveCourtAppearances()) {
          eventsToEmit.addAll(
            chargeService.deleteCharge(
              appearanceCharge.charge!!,
              courtAppearanceEntity.courtCase.prisonerId,
              courtAppearanceEntity.courtCase.caseUniqueIdentifier,
              courtAppearanceEntity.appearanceUuid.toString(),
            ).eventsToEmit,
          )
        }
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.removedFrom(appearanceCharge, serviceUserService.getUsername(), null))
        courtAppearanceEntity.appearanceCharges.remove(appearanceCharge)
        appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
        appearanceCharge.appearance = null
        appearanceCharge.charge = null
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

    if (courtCaseEntity.appearances.none { it.statusId == CourtAppearanceEntityStatus.ACTIVE || it.statusId == CourtAppearanceEntityStatus.FUTURE }) {
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
