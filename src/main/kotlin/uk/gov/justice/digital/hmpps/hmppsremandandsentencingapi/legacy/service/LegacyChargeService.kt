package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
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

    val performedByUsername = charge.performedByUser ?: serviceUserService.getUsername()
    val createdCharge = chargeRepository.save(ChargeEntity.from(charge, dpsOutcome, performedByUsername))
    chargeHistoryRepository.save(
      ChargeHistoryEntity.from(
        createdCharge,
        ChangeSource.NOMIS,
      ),
    )
    if (charge.legacyData.nomisOutcomeCode == null) {
      log.info("charge at ${createdCharge.chargeUuid} created in appearance ${charge.appearanceLifetimeUuid} with no outcome set")
    }
    val appearanceCharge = AppearanceChargeEntity(
      courtAppearance,
      createdCharge,
      performedByUsername,
      null,
    )
    courtAppearance.appearanceCharges.add(appearanceCharge)
    createdCharge.appearanceCharges.add(appearanceCharge)
    appearanceChargeHistoryRepository.save(
      AppearanceChargeHistoryEntity.from(
        appearanceCharge,
        ChangeSource.NOMIS,
      ),
    )
    return LegacyChargeCreatedResponse(createdCharge.chargeUuid, courtAppearance.courtCase.caseUniqueIdentifier, courtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun updateInAllAppearances(chargeUuid: UUID, charge: LegacyUpdateWholeCharge) {
    val existingChargeRecords = chargeRepository.findByChargeUuidAndStatusId(chargeUuid, ChargeEntityStatus.ACTIVE)
    if (existingChargeRecords.isEmpty()) {
      throw EntityNotFoundException("No charge found at $chargeUuid")
    }
    val performedByUsername = charge.performedByUser ?: serviceUserService.getUsername()
    existingChargeRecords.forEach { existingCharge ->
      val updatedCharge = existingCharge.copyFrom(charge, performedByUsername)
      if (!existingCharge.isSame(updatedCharge, existingCharge.getLiveSentence() != null)) {
        existingCharge.updateFrom(updatedCharge)
        chargeHistoryRepository.save(
          ChargeHistoryEntity.from(
            existingCharge,
            ChangeSource.NOMIS,
          ),
        )
      }
    }
  }

  @Transactional
  fun linkAppearanceAndUpdate(lifetimeUuid: UUID, appearanceUuid: UUID, charge: LegacyUpdateCharge): RecordResponse<LegacyChargeCreatedResponse> {
    val existingCourtAppearance = getAppearanceUnlessDeleted(appearanceUuid)
    val existingCharge = getUnlessDeleted(lifetimeUuid)
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    val (updateChargeEntityChangeStatus, legacyChargeCreatedResponse) = updateChargeInAppearance(existingCharge, lifetimeUuid, existingCourtAppearance, charge)
    if (updateChargeEntityChangeStatus == EntityChangeStatus.EDITED) {
      eventsToEmit.add(
        EventMetadataCreator.chargeEventMetadata(
          legacyChargeCreatedResponse.prisonerId,
          legacyChargeCreatedResponse.courtCaseUuid,
          appearanceUuid.toString(),
          legacyChargeCreatedResponse.lifetimeUuid.toString(),
          EventType.CHARGE_UPDATED,
        ),
      )
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          existingCourtAppearance.courtCase.prisonerId,
          existingCourtAppearance.courtCase.caseUniqueIdentifier,
          existingCourtAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    } else if (updateChargeEntityChangeStatus == EntityChangeStatus.NO_CHANGE && existingCourtAppearance.appearanceCharges.none { it.charge!!.chargeUuid == lifetimeUuid }) {
      val appearanceCharge = AppearanceChargeEntity(
        existingCourtAppearance,
        existingCharge,
        charge.performedByUser ?: serviceUserService.getUsername(),
        null,
      )
      existingCourtAppearance.appearanceCharges.add(appearanceCharge)
      existingCharge.appearanceCharges.add(appearanceCharge)
      appearanceChargeHistoryRepository.save(
        AppearanceChargeHistoryEntity.from(
          appearanceCharge,
          ChangeSource.NOMIS,
        ),
      )
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          existingCourtAppearance.courtCase.prisonerId,
          existingCourtAppearance.courtCase.caseUniqueIdentifier,
          existingCourtAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    }
    return RecordResponse(legacyChargeCreatedResponse, eventsToEmit)
  }

  @Transactional
  fun updateInAppearance(lifetimeUuid: UUID, appearanceUuid: UUID, charge: LegacyUpdateCharge): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    val existingCharge = getAtAppearanceUnlessDeleted(appearanceUuid, lifetimeUuid)
    val existingAppearance = existingCharge.appearanceCharges.first { it.appearance!!.appearanceUuid == appearanceUuid }.appearance!!
    return updateChargeInAppearance(existingCharge, lifetimeUuid, existingAppearance, charge)
  }

  private fun updateChargeInAppearance(existingCharge: ChargeEntity, lifetimeUuid: UUID, appearance: CourtAppearanceEntity, charge: LegacyUpdateCharge): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingOutcomeCode = existingCharge.chargeOutcome?.nomisCode ?: existingCharge.legacyData?.nomisOutcomeCode
    if (existingOutcomeCode != charge.legacyData.nomisOutcomeCode) {
      log.info("charge at $lifetimeUuid in appearance ${appearance.appearanceUuid} is updating outcome from $existingOutcomeCode to ${charge.legacyData.nomisOutcomeCode}")
    }
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val performedByUsername = charge.performedByUser ?: serviceUserService.getUsername()
    val updatedCharge = existingCharge.copyFrom(charge, dpsOutcome, performedByUsername)
    if (!existingCharge.isSame(updatedCharge, existingCharge.getLiveSentence() != null)) {
      createChargeRecordIfOverManyAppearancesOrUpdate(existingCharge, appearance, updatedCharge, performedByUsername) { charge ->
        charge.updateFrom(updatedCharge)
      }
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    val courtCase = appearance.courtCase
    return entityChangeStatus to LegacyChargeCreatedResponse(lifetimeUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getUnlessDeleted(lifetimeUUID))

  @Transactional(readOnly = true)
  fun getChargeAtAppearance(appearanceLifetimeUuid: UUID, lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getAtAppearanceUnlessDeleted(appearanceLifetimeUuid, lifetimeUUID))

  @Transactional
  fun delete(chargeUUID: UUID, performedByUsername: String?): LegacyCharge? = chargeRepository.findByChargeUuid(chargeUUID)
    .filter { it.statusId != ChargeEntityStatus.DELETED }
    .map { charge ->
      charge.delete(performedByUsername ?: serviceUserService.getUsername())
      chargeHistoryRepository.save(
        ChargeHistoryEntity.from(
          charge,
          ChangeSource.NOMIS,
        ),
      )
      val deletedManyChargesSentence = charge.sentences.filter { it.statusId == SentenceEntityStatus.MANY_CHARGES_DATA_FIX }.map {
        legacySentenceService.delete(it, performedByUsername ?: serviceUserService.getUsername())
        it
      }.firstOrNull()
      deletedManyChargesSentence to LegacyCharge.from(charge)
    }.firstOrNull()?.let { (deletedManyChargesSentence, legacyCharge) ->
      if (deletedManyChargesSentence != null) {
        legacySentenceService.handleManyChargesSentenceDeleted(deletedManyChargesSentence.sentenceUuid, performedByUsername ?: serviceUserService.getUsername())
      }
      legacyCharge
    }

  @Transactional
  fun linkChargeToCase(appearanceUuid: UUID, chargeUuid: UUID, linkChargeToCase: LegacyLinkChargeToCase): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCharge = getAtAppearanceUnlessDeleted(appearanceUuid, chargeUuid)
    val sourceCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(linkChargeToCase.sourceCourtCaseUuid)?.takeUnless { it.statusId == CourtCaseEntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${linkChargeToCase.sourceCourtCaseUuid}")
    val appearance = existingCharge.appearanceCharges.first { it.appearance!!.appearanceUuid == appearanceUuid }.appearance!!
    val performedByUsername = linkChargeToCase.performedByUser ?: serviceUserService.getUsername()
    val updatedCharge = existingCharge.copyFrom(linkChargeToCase, sourceCourtCase, performedByUsername)
    if (!existingCharge.isSame(updatedCharge, existingCharge.getLiveSentence() != null)) {
      var chargeRecord = existingCharge
      val chargeRecordsOnSourceCase = existingCharge.appearanceCharges.filter {
        it.appearance!!.courtCase == sourceCourtCase
      }
      if (chargeRecordsOnSourceCase.isNotEmpty()) {
        val appearanceChargeInTargetCase = existingCharge.appearanceCharges.first { it.appearance == appearance }
        appearanceChargeHistoryRepository.save(
          AppearanceChargeHistoryEntity.removedFrom(
            appearanceChargeInTargetCase,
            removedBy = performedByUsername,
            removedPrison = null,
            ChangeSource.NOMIS,
          ),
        )
        existingCharge.appearanceCharges.remove(appearanceChargeInTargetCase)
        chargeRecord.appearanceCharges.remove(appearanceChargeInTargetCase)
        appearance.appearanceCharges.remove(appearanceChargeInTargetCase)
        appearanceChargeInTargetCase.charge = null
        appearanceChargeInTargetCase.appearance = null
        chargeRecordsOnSourceCase.map { it.charge!! }.filter { it.statusId != ChargeEntityStatus.DELETED }.forEach { chargeRecordOnSourceCase ->
          chargeRecordOnSourceCase.statusId = ChargeEntityStatus.MERGED
          chargeRecordOnSourceCase.updatedBy = performedByUsername
          chargeRecordOnSourceCase.updatedAt = ZonedDateTime.now()
          chargeHistoryRepository.save(
            ChargeHistoryEntity.from(
              chargeRecordOnSourceCase,
              ChangeSource.NOMIS,
            ),
          )
        }
        chargeRecord = chargeRepository.save(updatedCharge)
        val appearanceCharge = AppearanceChargeEntity(
          appearance,
          chargeRecord,
          performedByUsername,
          null,
        )
        appearance.appearanceCharges.add(appearanceCharge)
        chargeRecord.appearanceCharges.add(appearanceCharge)
        appearanceChargeHistoryRepository.save(
          AppearanceChargeHistoryEntity.from(
            appearanceCharge,
            ChangeSource.NOMIS,
          ),
        )
      } else {
        existingCharge.updateFrom(updatedCharge)
      }
      chargeHistoryRepository.save(
        ChargeHistoryEntity.from(
          chargeRecord,
          ChangeSource.NOMIS,
        ),
      )
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyChargeCreatedResponse(chargeUuid, appearance.courtCase.caseUniqueIdentifier, appearance.courtCase.prisonerId)
  }

  private fun getAtAppearanceUnlessDeleted(appearanceUuid: UUID, chargeUuid: UUID): ChargeEntity = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid,
    chargeUuid,
  ) ?: throw EntityNotFoundException("No charge found at $chargeUuid for appearance $appearanceUuid")

  private fun getUnlessDeleted(lifetimeUUID: UUID): ChargeEntity = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(lifetimeUUID) ?: throw EntityNotFoundException("No charge found at $lifetimeUUID")

  private fun getAppearanceUnlessDeleted(appearanceUuid: UUID): CourtAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)
    ?.takeUnless { entity -> entity.statusId == CourtAppearanceEntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at $appearanceUuid")

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
