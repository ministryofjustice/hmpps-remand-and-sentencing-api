package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyChargeCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyLinkChargeToCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.ZonedDateTime
import java.util.UUID

@Service
class LegacyChargeService(
  chargeRepository: ChargeRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val chargeOutcomeRepository: ChargeOutcomeRepository,
  serviceUserService: ServiceUserService,
  chargeHistoryRepository: ChargeHistoryRepository,
  appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
  private val legacySentenceService: LegacySentenceService,
  private val courtCaseRepository: CourtCaseRepository,
) : LegacyBaseService(chargeRepository, appearanceChargeHistoryRepository, chargeHistoryRepository, serviceUserService) {

  @Transactional
  fun create(charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    val courtAppearance = courtAppearanceRepository.findByAppearanceUuid(charge.appearanceLifetimeUuid)?.takeUnless { entity -> entity.statusId == CourtAppearanceEntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at ${charge.appearanceLifetimeUuid}")
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val createdCharge = chargeRepository.save(ChargeEntity.from(charge, dpsOutcome, serviceUserService.getUsername()))
    chargeHistoryRepository.save(ChargeHistoryEntity.from(createdCharge))
    val appearanceCharge = AppearanceChargeEntity(
      courtAppearance,
      createdCharge,
      serviceUserService.getUsername(),
      null,
    )
    courtAppearance.appearanceCharges.add(appearanceCharge)
    createdCharge.appearanceCharges.add(appearanceCharge)
    appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
    return LegacyChargeCreatedResponse(createdCharge.chargeUuid, courtAppearance.courtCase.caseUniqueIdentifier, courtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun updateInAllAppearances(chargeUuid: UUID, charge: LegacyUpdateWholeCharge) {
    val existingChargeRecords = chargeRepository.findByChargeUuidAndStatusId(chargeUuid, ChargeEntityStatus.ACTIVE)
    if (existingChargeRecords.isEmpty()) {
      throw EntityNotFoundException("No charge found at $chargeUuid")
    }
    existingChargeRecords.forEach { existingCharge ->
      val updatedCharge = existingCharge.copyFrom(charge, serviceUserService.getUsername())
      if (!existingCharge.isSame(updatedCharge, existingCharge.getActiveOrInactiveSentence() != null)) {
        existingCharge.updateFrom(updatedCharge)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge))
      }
    }
  }

  @Transactional
  fun updateInAppearance(lifetimeUuid: UUID, appearanceUuid: UUID, charge: LegacyUpdateCharge): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCharge = getAtAppearanceUnlessDeleted(appearanceUuid, lifetimeUuid)
    val appearance = existingCharge.appearanceCharges.first { it.appearance!!.appearanceUuid == appearanceUuid }.appearance!!
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val updatedCharge = existingCharge.copyFrom(charge, dpsOutcome, serviceUserService.getUsername())
    if (!existingCharge.isSame(updatedCharge, existingCharge.getActiveOrInactiveSentence() != null)) {
      createChargeRecordIfOverManyAppearancesOrUpdate(existingCharge, appearance, updatedCharge) { charge ->
        charge.updateFrom(updatedCharge)
      }
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    val courtCase = existingCharge.appearanceCharges.first().appearance!!.courtCase
    return entityChangeStatus to LegacyChargeCreatedResponse(lifetimeUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getUnlessDeleted(lifetimeUUID))

  @Transactional(readOnly = true)
  fun getChargeAtAppearance(appearanceLifetimeUuid: UUID, lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getAtAppearanceUnlessDeleted(appearanceLifetimeUuid, lifetimeUUID))

  @Transactional
  fun delete(chargeUUID: UUID): LegacyCharge? = chargeRepository.findByChargeUuid(chargeUUID)
    .filter { it.statusId != ChargeEntityStatus.DELETED }
    .map { charge ->
      charge.delete(serviceUserService.getUsername())
      chargeHistoryRepository.save(ChargeHistoryEntity.from(charge))
      val deletedManyChargesSentence = charge.sentences.filter { it.statusId == SentenceEntityStatus.MANY_CHARGES_DATA_FIX }.map {
        legacySentenceService.delete(it)
        it
      }.firstOrNull()
      deletedManyChargesSentence to LegacyCharge.from(charge)
    }.firstOrNull()?.let { (deletedManyChargesSentence, legacyCharge) ->
      if (deletedManyChargesSentence != null) {
        legacySentenceService.handleManyChargesSentenceDeleted(deletedManyChargesSentence.sentenceUuid)
      }
      legacyCharge
    }

  @Transactional
  fun linkChargeToCase(appearanceUuid: UUID, chargeUuid: UUID, linkChargeToCase: LegacyLinkChargeToCase): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCharge = getAtAppearanceUnlessDeleted(appearanceUuid, chargeUuid)
    val sourceCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(linkChargeToCase.sourceCourtCaseUuid)?.takeUnless { it.statusId == CourtCaseEntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${linkChargeToCase.sourceCourtCaseUuid}")
    val appearance = existingCharge.appearanceCharges.first { it.appearance!!.appearanceUuid == appearanceUuid }.appearance!!
    val updatedCharge = existingCharge.copyFrom(linkChargeToCase, sourceCourtCase, serviceUserService.getUsername())
    if (!existingCharge.isSame(updatedCharge, existingCharge.getActiveOrInactiveSentence() != null)) {
      var chargeRecord = existingCharge
      val chargeRecordsOnSourceCase = existingCharge.appearanceCharges.filter {
        it.appearance!!.courtCase == sourceCourtCase
      }
      if (chargeRecordsOnSourceCase.isNotEmpty()) {
        val appearanceChargeInTargetCase = existingCharge.appearanceCharges.first { it.appearance == appearance }
        appearanceChargeHistoryRepository.save(
          AppearanceChargeHistoryEntity.removedFrom(
            appearanceChargeInTargetCase,
            removedBy = serviceUserService.getUsername(),
            removedPrison = null,
          ),
        )
        existingCharge.appearanceCharges.remove(appearanceChargeInTargetCase)
        chargeRecord.appearanceCharges.remove(appearanceChargeInTargetCase)
        appearance.appearanceCharges.remove(appearanceChargeInTargetCase)
        appearanceChargeInTargetCase.charge = null
        appearanceChargeInTargetCase.appearance = null
        chargeRecordsOnSourceCase.map { it.charge!! }.filter { it.statusId != ChargeEntityStatus.DELETED }.forEach { chargeRecordOnSourceCase ->
          chargeRecordOnSourceCase.statusId = ChargeEntityStatus.MERGED
          chargeRecordOnSourceCase.updatedBy = serviceUserService.getUsername()
          chargeRecordOnSourceCase.updatedAt = ZonedDateTime.now()
          chargeHistoryRepository.save(ChargeHistoryEntity.from(chargeRecordOnSourceCase))
        }
        chargeRecord = chargeRepository.save(updatedCharge)
        val appearanceCharge = AppearanceChargeEntity(
          appearance,
          chargeRecord,
          serviceUserService.getUsername(),
          null,
        )
        appearance.appearanceCharges.add(appearanceCharge)
        chargeRecord.appearanceCharges.add(appearanceCharge)
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
      } else {
        existingCharge.updateFrom(updatedCharge)
      }
      chargeHistoryRepository.save(ChargeHistoryEntity.from(chargeRecord))
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyChargeCreatedResponse(chargeUuid, appearance.courtCase.caseUniqueIdentifier, appearance.courtCase.prisonerId)
  }

  private fun getAtAppearanceUnlessDeleted(appearanceUuid: UUID, chargeUuid: UUID): ChargeEntity = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid,
    chargeUuid,
  ) ?: throw EntityNotFoundException("No charge found at $chargeUuid for appearance $appearanceUuid")

  private fun getUnlessDeleted(lifetimeUUID: UUID): ChargeEntity = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(lifetimeUUID) ?: throw EntityNotFoundException("No charge found at $lifetimeUUID")
}
