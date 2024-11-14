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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
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
  private val snsService: SnsService,
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
      val courtAppearance = createCourtAppearance(createCourtAppearance, courtCaseEntity, courtAppearanceRepository.findByAppearanceUuid(appearanceUuid))
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + courtAppearance)
      return courtAppearance
    }
  }

  @Transactional
  fun createCourtAppearanceByLifetimeUuid(createCourtAppearance: CreateCourtAppearance, lifetimeUuid: UUID): CourtAppearanceEntity? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val courtAppearance = createCourtAppearance(createCourtAppearance, courtCaseEntity, courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(lifetimeUuid))
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + courtAppearance)
      return courtAppearance
    }
  }

  @Transactional
  fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): CourtAppearanceEntity {
    return createCourtAppearance(courtAppearance, courtCaseEntity, courtAppearanceRepository.findByAppearanceUuid(courtAppearance.appearanceUuid))
  }

  private fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity, existingCourtAppearanceEntity: CourtAppearanceEntity?): CourtAppearanceEntity {
    var appearanceLegacyData = courtAppearance.legacyData
    val appearanceOutcome = courtAppearance.outcomeUuid?.let {
      appearanceLegacyData = appearanceLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null)
      appearanceOutcomeRepository.findByOutcomeUuid(it)
    } ?: appearanceLegacyData?.nomisOutcomeCode?.let { appearanceOutcomeRepository.findByNomisCode(it) }
    val sentencesCreated = mutableMapOf<String, SentenceEntity>()
    val charges = courtAppearance.charges.sortedWith(this::chargesByConsecutiveToLast).map {
      val charge = chargeService.createCharge(it, sentencesCreated, courtCaseEntity.prisonerId)
      charge.getActiveSentence()?.let { sentence -> sentencesCreated.put(sentence.chargeNumber, sentence) }
      charge
    }.toMutableSet()
    val legacyData = appearanceLegacyData?.let { objectMapper.valueToTree<JsonNode>(it) }
    val (toCreateAppearance, status) = existingCourtAppearanceEntity?.let { courtAppearanceEntity ->
      if (courtAppearanceEntity.statusId == EntityStatus.EDITED) {
        throw ImmutableCourtAppearanceException("Cannot edit an already edited court appearance")
      }
      val compareAppearance = CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges, legacyData)
      if (courtAppearanceEntity.isSame(compareAppearance)) {
        val toDeleteCharges = courtAppearanceEntity.charges.filter { existingCharge -> courtAppearance.charges.none { it.chargeUuid == existingCharge.chargeUuid } }
        toDeleteCharges.forEach { chargeService.deleteCharge(it, courtCaseEntity.prisonerId) }

        courtAppearanceEntity.charges.addAll(charges)
        return@let courtAppearanceEntity to EntityChangeStatus.NO_CHANGE
      }
      courtAppearanceEntity.statusId = EntityStatus.EDITED
      compareAppearance.previousAppearance = courtAppearanceEntity
      compareAppearance.appearanceUuid = UUID.randomUUID()
      courtAppearance.appearanceUuid = compareAppearance.appearanceUuid
      compareAppearance.lifetimeUuid = courtAppearanceEntity.lifetimeUuid
      compareAppearance to EntityChangeStatus.EDITED
    } ?: (CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges, legacyData) to EntityChangeStatus.CREATED)

    val nextCourtAppearance = courtAppearance.nextCourtAppearance?.let { NextCourtAppearanceEntity.from(it) }
    if (toCreateAppearance.nextCourtAppearance?.isSame(nextCourtAppearance) != true) {
      val toSaveNextCourtAppearance = nextCourtAppearance?.let { nextCourtAppearanceRepository.save(it) }
      toCreateAppearance.nextCourtAppearance = toSaveNextCourtAppearance
    }
    updateDocumentMetadata(toCreateAppearance, courtCaseEntity.prisonerId)
    val toCreatePeriodLengths = toCreateAppearance.periodLengths.filter { it.id == 0 }
    toCreateAppearance.periodLengths = toCreateAppearance.periodLengths.filter { it.id != 0 }

    val createdCourtAppearance = courtAppearanceRepository.save(toCreateAppearance)
    toCreatePeriodLengths.forEach {
      it.appearanceEntity = createdCourtAppearance
      periodLengthRepository.save(it)
    }
    if (status == EntityChangeStatus.CREATED) {
      snsService.courtAppearanceInserted(createdCourtAppearance.courtCase.prisonerId, createdCourtAppearance.appearanceUuid.toString(), createdCourtAppearance.courtCase.caseUniqueIdentifier, createdCourtAppearance.createdAt)
    } else if (status == EntityChangeStatus.EDITED) {
      snsService.courtAppearanceUpdated(createdCourtAppearance.courtCase.prisonerId, createdCourtAppearance.appearanceUuid.toString(), createdCourtAppearance.courtCase.caseUniqueIdentifier, createdCourtAppearance.createdAt)
    }
    return createdCourtAppearance
  }

  @Transactional
  fun createChargeInAppearance(createCharge: CreateCharge): ChargeEntity? {
    return courtAppearanceRepository.findByAppearanceUuid(createCharge.appearanceUuid!!)?.let { courtAppearance ->
      val sentencesCreated = courtAppearance.charges.filter { it.getActiveSentence() != null }.associate { charge ->
        val sentence = charge.getActiveSentence()!!
        sentence.chargeNumber to sentence
      }
      val charge = chargeService.createCharge(createCharge, sentencesCreated, courtAppearance.courtCase.prisonerId)
      courtAppearance.charges.add(charge)
      charge
    }
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
  fun deleteCourtAppearanceByLifetime(lifetimeUuid: UUID) = courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(lifetimeUuid)?.let { deleteCourtAppearance(it) }

  @Transactional
  fun deleteCourtAppearance(courtAppearanceEntity: CourtAppearanceEntity) {
    courtAppearanceEntity.statusId = EntityStatus.DELETED
    courtAppearanceEntity.charges.filter { it.hasNoActiveCourtAppearances() }.forEach { charge -> chargeService.deleteCharge(charge, courtAppearanceEntity.courtCase.prisonerId) }
    snsService.courtAppearanceDeleted(courtAppearanceEntity.courtCase.prisonerId, courtAppearanceEntity.appearanceUuid.toString(), courtAppearanceEntity.courtCase.caseUniqueIdentifier, courtAppearanceEntity.createdAt)
  }

  @Transactional
  fun disassociateChargeWithAppearance(appearanceUuid: UUID, chargeUuid: UUID) = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { courtAppearanceEntity ->
    val chargeToRemove = courtAppearanceEntity.charges.find { it.chargeUuid == chargeUuid }
    chargeToRemove?.let { chargeEntity ->
      courtAppearanceEntity.charges.remove(chargeEntity)
      chargeEntity.courtAppearances.remove(courtAppearanceEntity)
      chargeEntity.takeIf { it.hasNoActiveCourtAppearances() }?.let { chargeService.deleteCharge(it, courtAppearanceEntity.courtCase.prisonerId) }
    }
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): CourtAppearance? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { CourtAppearance.from(it) }

  @Transactional(readOnly = true)
  fun findAppearanceByLifetimeUuid(lifetimeUuid: UUID): CourtAppearance? = courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(lifetimeUuid)?.let { CourtAppearance.from(it) }
}
