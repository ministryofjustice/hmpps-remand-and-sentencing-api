package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
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
  private val courtAppearanceDomainEventService: CourtAppearanceDomainEventService,
  private val objectMapper: ObjectMapper,
) {

  @Transactional
  fun createCourtAppearance(createCourtAppearance: CreateCourtAppearance): CourtAppearanceEntity? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val courtAppearance = createCourtAppearance(createCourtAppearance, courtCaseEntity)
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + courtAppearance)
      return courtAppearance
    }
  }

  @Transactional
  fun createCourtAppearanceByAppearanceUuid(createCourtAppearance: CreateCourtAppearance, appearanceUuid: UUID): CourtAppearanceEntity? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val existingCourtAppearance = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)

      val savedAppearance = if (existingCourtAppearance != null) updateCourtAppearanceEntity(createCourtAppearance, courtCaseEntity, existingCourtAppearance) else createCourtAppearanceEntity(createCourtAppearance, courtCaseEntity)
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + savedAppearance)
      return savedAppearance
    }
  }

  @Transactional
  fun createCourtAppearanceByLifetimeUuid(createCourtAppearance: CreateCourtAppearance, lifetimeUuid: UUID): CourtAppearanceEntity? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val existingCourtAppearance = courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(lifetimeUuid)
      val savedAppearance = if (existingCourtAppearance != null) updateCourtAppearanceEntity(createCourtAppearance, courtCaseEntity, existingCourtAppearance) else createCourtAppearanceEntity(createCourtAppearance, courtCaseEntity)
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + savedAppearance)
      return savedAppearance
    }
  }

  @Transactional
  fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): CourtAppearanceEntity {
    return courtAppearanceRepository.findByAppearanceUuid(courtAppearance.appearanceUuid)?.let { existingCourtAppearance ->
      updateCourtAppearanceEntity(courtAppearance, courtCaseEntity, existingCourtAppearance)
    } ?: createCourtAppearanceEntity(courtAppearance, courtCaseEntity)
  }

  private fun createCourtAppearanceEntity(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): CourtAppearanceEntity {
    val createdCharges = createCharges(courtAppearance.charges, courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier)
    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    val legacyData = appearanceLegacyData?.let { objectMapper.valueToTree<JsonNode>(it) }
    val nextCourtAppearance = courtAppearance.nextCourtAppearance?.let { nextCourtAppearance ->
      val futureCourtAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.fromFuture(nextCourtAppearance, courtCaseEntity, serviceUserService.getUsername(), courtAppearance.courtCaseReference))
      nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextCourtAppearance, futureCourtAppearance),
      )
    }
    val createdCourtAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), createdCharges, legacyData))
    createdCourtAppearance.nextCourtAppearance = nextCourtAppearance
    courtAppearance.overallSentenceLength?.let { createPeriodLength ->
      val periodLength = PeriodLengthEntity.from(createPeriodLength)
      periodLength.appearanceEntity = createdCourtAppearance
      val createdPeriodLength = periodLengthRepository.save(periodLength)
      createdCourtAppearance.periodLengths = listOf(createdPeriodLength)
    }
    updateDocumentMetadata(createdCourtAppearance, courtCaseEntity.prisonerId)

    courtAppearanceDomainEventService.create(createdCourtAppearance.courtCase.prisonerId, createdCourtAppearance.lifetimeUuid.toString(), createdCourtAppearance.courtCase.caseUniqueIdentifier, "DPS")
    nextCourtAppearance?.futureSkeletonAppearance?.let { courtAppearanceEntity -> courtAppearanceDomainEventService.create(courtAppearanceEntity.courtCase.prisonerId, courtAppearanceEntity.lifetimeUuid.toString(), courtAppearanceEntity.courtCase.caseUniqueIdentifier, "DPS") }
    return createdCourtAppearance
  }

  private fun updateCourtAppearanceEntity(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity, existingCourtAppearanceEntity: CourtAppearanceEntity): CourtAppearanceEntity {
    if (existingCourtAppearanceEntity.statusId == EntityStatus.EDITED) {
      throw ImmutableCourtAppearanceException("Cannot edit an already edited court appearance")
    }
    var appearanceChangeStatus = EntityChangeStatus.NO_CHANGE
    val (chargesChangedStatus, charges) = updateCharges(courtAppearance.charges, courtCaseEntity.prisonerId, courtCaseEntity.caseUniqueIdentifier, existingCourtAppearanceEntity)
    val (appearanceLegacyData, appearanceOutcome) = getAppearanceOutcome(courtAppearance)
    val legacyData = appearanceLegacyData?.let { objectMapper.valueToTree<JsonNode>(it) }
    val compareAppearance = existingCourtAppearanceEntity.copyFrom(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges, legacyData)
    var activeRecord = existingCourtAppearanceEntity

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
    val (nextCourtAppearanceEntityChangeStatus, futureSkeletonAppearance) = updateNextCourtAppearance(courtAppearance, activeRecord)
    updateDocumentMetadata(activeRecord, courtCaseEntity.prisonerId)
    if (appearanceChangeStatus == EntityChangeStatus.EDITED || chargesChangedStatus == EntityChangeStatus.EDITED) {
      courtAppearanceDomainEventService.update(activeRecord.courtCase.prisonerId, activeRecord.lifetimeUuid.toString(), activeRecord.courtCase.caseUniqueIdentifier, "DPS")
    }

    if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.CREATED) {
      courtAppearanceDomainEventService.create(futureSkeletonAppearance!!.courtCase.prisonerId, futureSkeletonAppearance.lifetimeUuid.toString(), futureSkeletonAppearance.courtCase.caseUniqueIdentifier, "DPS")
    } else if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.EDITED) {
      courtAppearanceDomainEventService.update(futureSkeletonAppearance!!.courtCase.prisonerId, futureSkeletonAppearance.lifetimeUuid.toString(), futureSkeletonAppearance.courtCase.caseUniqueIdentifier, "DPS")
    } else if (nextCourtAppearanceEntityChangeStatus == EntityChangeStatus.DELETED) {
      courtAppearanceDomainEventService.delete(futureSkeletonAppearance!!.courtCase.prisonerId, futureSkeletonAppearance.lifetimeUuid.toString(), futureSkeletonAppearance.courtCase.caseUniqueIdentifier, "DPS")
    }

    return activeRecord
  }

  private fun updateNextCourtAppearance(
    courtAppearance: CreateCourtAppearance,
    activeRecord: CourtAppearanceEntity,
  ): Pair<EntityChangeStatus, CourtAppearanceEntity?> {
    return activeRecord.nextCourtAppearance?.let { activeNextCourtAppearance ->
      if (courtAppearance.nextCourtAppearance != null) {
        val activeFutureSkeletonAppearance = activeNextCourtAppearance.futureSkeletonAppearance
        var futureCourtAppearance = activeFutureSkeletonAppearance.copyFromFuture(
          courtAppearance.nextCourtAppearance,
          activeRecord.courtCase,
          serviceUserService.getUsername(),
          courtAppearance.courtCaseReference,
        )
        val nextCourtAppearance =
          NextCourtAppearanceEntity.from(courtAppearance.nextCourtAppearance, futureCourtAppearance)
        if (!activeNextCourtAppearance.isSame(nextCourtAppearance)) {
          val savedFutureCourtAppearance = courtAppearanceRepository.save(futureCourtAppearance)
          activeFutureSkeletonAppearance.statusId = EntityStatus.EDITED
          activeRecord.nextCourtAppearance = nextCourtAppearanceRepository.save(
            NextCourtAppearanceEntity.from(
              courtAppearance.nextCourtAppearance,
              savedFutureCourtAppearance,
            ),
          )
          return@let EntityChangeStatus.EDITED to savedFutureCourtAppearance
        }
        EntityChangeStatus.NO_CHANGE to null
      } else {
        activeNextCourtAppearance.futureSkeletonAppearance.statusId = EntityStatus.DELETED
        activeRecord.nextCourtAppearance = null
        EntityChangeStatus.DELETED to activeNextCourtAppearance.futureSkeletonAppearance
      }
    } ?: courtAppearance.nextCourtAppearance?.let { toCreateNextCourtAppearance ->
      val futureCourtAppearance = courtAppearanceRepository.save(
        CourtAppearanceEntity.fromFuture(
          toCreateNextCourtAppearance,
          activeRecord.courtCase,
          serviceUserService.getUsername(),
          courtAppearance.courtCaseReference,
        ),
      )
      val savedNextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(toCreateNextCourtAppearance, futureCourtAppearance),
      )
      activeRecord.nextCourtAppearance = savedNextCourtAppearance
      EntityChangeStatus.CREATED to futureCourtAppearance
    } ?: (EntityChangeStatus.NO_CHANGE to null)
  }

  private fun updateCharges(charges: List<CreateCharge>, prisonerId: String, courtCaseUuid: String, existingCourtAppearanceEntity: CourtAppearanceEntity): Pair<EntityChangeStatus, MutableSet<ChargeEntity>> {
    val createdCharges = createCharges(charges, prisonerId, courtCaseUuid)

    val toDeleteCharges = existingCourtAppearanceEntity.charges.filter { existingCharge -> charges.none { createCharge -> createCharge.chargeUuid == existingCharge.chargeUuid } }
    toDeleteCharges.forEach { chargeEntity ->
      chargeEntity.courtAppearances.removeIf { it.id == existingCourtAppearanceEntity.id }
      chargeService.deleteChargeIfOrphan(chargeEntity, prisonerId, courtCaseUuid)
    }
    existingCourtAppearanceEntity.charges.removeAll(toDeleteCharges)
    val toAddCharges = createdCharges.filter { chargeEntity -> existingCourtAppearanceEntity.charges.none { existingCharge -> chargeEntity.chargeUuid == existingCharge.chargeUuid } }
    existingCourtAppearanceEntity.charges.addAll(toAddCharges)
    val entityChangeStatus = if (toAddCharges.isNotEmpty() || toDeleteCharges.isNotEmpty()) EntityChangeStatus.EDITED else EntityChangeStatus.NO_CHANGE

    return entityChangeStatus to createdCharges
  }

  private fun createCharges(
    charges: List<CreateCharge>,
    prisonerId: String,
    courtCaseUuid: String,
  ): MutableSet<ChargeEntity> {
    val sentencesCreated = mutableMapOf<String, SentenceEntity>()
    return charges.sortedWith(this::chargesByConsecutiveToLast).map {
      val charge = chargeService.createCharge(it, sentencesCreated, prisonerId, courtCaseUuid)
      charge.getActiveSentence()?.let { sentence -> sentencesCreated.put(sentence.chargeNumber, sentence) }
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
  fun deleteCourtAppearance(appearanceUuid: UUID) = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { deleteCourtAppearance(it) }

  @Transactional
  fun deleteCourtAppearance(courtAppearanceEntity: CourtAppearanceEntity) {
    courtAppearanceEntity.statusId = EntityStatus.DELETED
    courtAppearanceEntity.charges.filter { it.hasNoActiveCourtAppearances() }.forEach { charge -> chargeService.deleteCharge(charge, courtAppearanceEntity.courtCase.prisonerId, courtAppearanceEntity.courtCase.caseUniqueIdentifier) }
    courtAppearanceDomainEventService.delete(courtAppearanceEntity.courtCase.prisonerId, courtAppearanceEntity.lifetimeUuid.toString(), courtAppearanceEntity.courtCase.caseUniqueIdentifier, "DPS")
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): CourtAppearance? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { CourtAppearance.from(it) }
}
