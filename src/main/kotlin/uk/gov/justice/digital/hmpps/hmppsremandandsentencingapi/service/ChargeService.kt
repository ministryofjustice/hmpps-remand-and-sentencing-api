package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.CourtCaseHierarchyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.AppearanceDeletedException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.util.UUID

@Service
class ChargeService(
  private val chargeRepository: ChargeRepository,
  private val chargeOutcomeRepository: ChargeOutcomeRepository,
  private val sentenceService: SentenceService,
  private val serviceUserService: ServiceUserService,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val aggravatingFactorsService: AggravatingFactorsService,
) {

  private fun createChargeEntity(
    charge: CreateCharge,
    sentencesCreated: MutableMap<UUID, SentenceEntity>,
    courtCaseHierarchyData: CourtCaseHierarchyData,
    supersedingCharge: ChargeEntity?,
  ): RecordResponse<ChargeEntity> {
    val chargeToSupersede: ChargeEntity? = supersedingCharge ?: charge.replacingChargeUuid?.let { findCharge(courtCaseHierarchyData.courtAppearanceUuid!!, it) }
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    charge.legacyData = chargeLegacyData
    val savedCharge = chargeRepository.save(
      ChargeEntity.from(
        charge,
        chargeOutcome,
        serviceUserService.getUsername(),
        chargeToSupersede,
      ),
    )
    aggravatingFactorsService.replaceAggravatingFactors(savedCharge, charge.aggravatingFactors.map { it.code }.toSet())
    chargeHistoryRepository.save(ChargeHistoryEntity.from(savedCharge, ChangeSource.DPS))
    val eventsToEmit = mutableSetOf(
      EventMetadataCreator.chargeEventMetadata(
        courtCaseHierarchyData.prisonerId,
        courtCaseHierarchyData.courtCaseId!!,
        courtCaseHierarchyData.courtAppearanceUuid.toString(),
        savedCharge.chargeUuid.toString(),
        EventType.CHARGE_INSERTED,
        false,
        courtCaseHierarchyData.isBreach,
      ),
    )
    charge.sentence?.let { createSentence ->
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(
        createSentence,
        savedCharge,
        sentencesCreated,
        courtCaseHierarchyData,
      )
      savedCharge.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    }
    return RecordResponse(savedCharge, eventsToEmit)
  }

  private fun updateChargeEntity(
    charge: CreateCharge,
    sentencesCreated: MutableMap<UUID, SentenceEntity>,
    existingCharge: ChargeEntity,
    courtAppearance: CourtAppearanceEntity,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<ChargeEntity> {
    val chargeChanges = mutableListOf<Pair<EntityChangeStatus, ChargeEntity>>()
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    charge.legacyData = chargeLegacyData
    var compareCharge = existingCharge.copyFrom(charge, chargeOutcome, serviceUserService.getUsername())
    var activeRecord = existingCharge
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()

    if (!existingCharge.isSame(compareCharge, charge.sentence != null)) {
      if (existingCharge.offenceCode != compareCharge.offenceCode) {
        val replacedWithAnotherOutcome = chargeOutcomeRepository.findByOutcomeUuid(replacedWithAnotherOutcomeUuid)
        if (existingCharge.hasTwoOrMoreLiveCourtAppearance(courtAppearance)) {
          courtAppearance.appearanceCharges.filter { it.charge == existingCharge }
            .forEach { appearanceCharge ->
              appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
              appearanceCharge.appearance!!.appearanceCharges.remove(appearanceCharge)
              appearanceChargeHistoryRepository.save(
                AppearanceChargeHistoryEntity.removedFrom(
                  appearanceCharge = appearanceCharge,
                  removedBy = serviceUserService.getUsername(),
                  removedPrison = charge.prisonId,
                  ChangeSource.DPS,
                ),
              )
              compareCharge.appearanceCharges.remove(appearanceCharge)
              appearanceCharge.charge = null
              appearanceCharge.appearance = null
            }
          val replacedWithAnotherRecord = existingCharge.copyFrom(replacedWithAnotherOutcome, serviceUserService.getUsername())
          replacedWithAnotherRecord.appearanceCharges.removeAll { it.appearance == null }
          activeRecord = chargeRepository.save(replacedWithAnotherRecord)
          val codesForReplacedRecord = activeRecord.chargeAggravatingFactors.map { it.aggravatingFactor.code }.toSet()
          aggravatingFactorsService.replaceAggravatingFactors(activeRecord, codesForReplacedRecord)
          chargeHistoryRepository.save(ChargeHistoryEntity.from(activeRecord, ChangeSource.DPS))
        } else {
          existingCharge.updateFrom(replacedWithAnotherOutcome, serviceUserService.getUsername(), charge.prisonId)
          chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge, ChangeSource.DPS))
        }
        val appearanceChargeEntity = AppearanceChargeEntity(
          courtAppearance,
          activeRecord,
          serviceUserService.getUsername(),
          charge.prisonId,
        )
        courtAppearance.appearanceCharges.add(appearanceChargeEntity)
        activeRecord.appearanceCharges.add(appearanceChargeEntity)
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity, ChangeSource.DPS))
        chargeChanges.add(EntityChangeStatus.EDITED to activeRecord)

        val newChargeRecord = chargeRepository.save(compareCharge.copyFromReplacedCharge(activeRecord))
        chargeHistoryRepository.save(ChargeHistoryEntity.from(newChargeRecord, ChangeSource.DPS))
        activeRecord = newChargeRecord
        chargeChanges.add(EntityChangeStatus.CREATED to newChargeRecord)
        eventsToEmit.addAll(
          sentenceService.moveSentencesToNewCharge(
            existingCharge,
            newChargeRecord,
            courtCaseHierarchyData,
          ),
        )
      } else if (existingCharge.hasTwoOrMoreLiveCourtAppearance(courtAppearance)) {
        courtAppearance.appearanceCharges.filter { it.charge == existingCharge }
          .forEach { appearanceCharge ->
            appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
            appearanceCharge.appearance!!.appearanceCharges.remove(appearanceCharge)
            appearanceChargeHistoryRepository.save(
              AppearanceChargeHistoryEntity.removedFrom(
                appearanceCharge = appearanceCharge,
                removedBy = serviceUserService.getUsername(),
                removedPrison = charge.prisonId,
                ChangeSource.DPS,
              ),
            )
            appearanceCharge.charge = null
            appearanceCharge.appearance = null
          }
        compareCharge.appearanceCharges.removeAll { it.appearance == null }
        activeRecord = chargeRepository.save(compareCharge)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(activeRecord, ChangeSource.DPS))
        chargeChanges.add(EntityChangeStatus.EDITED to activeRecord)
      } else {
        existingCharge.updateFrom(compareCharge)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge, ChangeSource.DPS))
        chargeChanges.add(EntityChangeStatus.EDITED to existingCharge)
      }
    }
    aggravatingFactorsService.replaceAggravatingFactors(activeRecord, charge.aggravatingFactors.map { it.code }.toSet())
    if (charge.sentence != null) {
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(
        charge.sentence,
        activeRecord,
        sentencesCreated,
        courtCaseHierarchyData,
      )
      activeRecord.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    } else {
      activeRecord.getLiveSentence()?.let { sentenceEntity ->
        eventsToEmit.addAll(
          sentenceService.deleteSentence(
            sentenceEntity,
            activeRecord,
            courtCaseHierarchyData,
          ).eventsToEmit,
        )
      }
    }
    chargeChanges.forEach { (chargeChangeStatus, record) ->
      if (chargeChangeStatus == EntityChangeStatus.EDITED) {
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            courtCaseHierarchyData.prisonerId,
            courtCaseHierarchyData.courtCaseId!!,
            courtCaseHierarchyData.courtAppearanceUuid.toString(),
            record.chargeUuid.toString(),
            EventType.CHARGE_UPDATED,
            courtAppearance.statusId == CourtAppearanceEntityStatus.FUTURE,
            courtCaseHierarchyData.isBreach,
          ),
        )
      } else if (chargeChangeStatus == EntityChangeStatus.CREATED) {
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            courtCaseHierarchyData.prisonerId,
            courtCaseHierarchyData.courtCaseId!!,
            courtCaseHierarchyData.courtAppearanceUuid.toString(),
            record.chargeUuid.toString(),
            EventType.CHARGE_INSERTED,
            courtAppearance.statusId == CourtAppearanceEntityStatus.FUTURE,
            courtCaseHierarchyData.isBreach,
          ),
        )
      }
    }
    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun getChargeOutcome(charge: CreateCharge): Pair<ChargeLegacyData?, ChargeOutcomeEntity?> {
    var chargeLegacyData = charge.legacyData
    val chargeOutcome = charge.outcomeUuid?.let {
      chargeLegacyData =
        chargeLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null)
      chargeOutcomeRepository.findByOutcomeUuid(it)
    } ?: chargeLegacyData?.nomisOutcomeCode?.let { chargeOutcomeRepository.findByNomisCode(it) }
    return chargeLegacyData to chargeOutcome
  }

  @Transactional
  fun createCharge(
    charge: CreateCharge,
    sentencesCreated: MutableMap<UUID, SentenceEntity>,
    courtAppearance: CourtAppearanceEntity,
    supersedingCharge: ChargeEntity? = null,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<ChargeEntity> {
    val existingCharge = findCharge(courtAppearance.appearanceUuid, charge.chargeUuid)
    val charge = if (existingCharge != null) {
      updateChargeEntity(
        charge,
        sentencesCreated,
        existingCharge,
        courtAppearance,
        courtCaseHierarchyData,
      )
    } else {
      createChargeEntity(charge, sentencesCreated, courtCaseHierarchyData, supersedingCharge)
    }
    return charge
  }

  @Transactional
  fun createFutureDatedCharge(
    existingCharge: ChargeEntity,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<ChargeEntity> {
    val futureCharge = existingCharge.toFutureCharge()
    val existingAggravatingFactorCodes = existingCharge.chargeAggravatingFactors.map { it.aggravatingFactor.code }.toSet()
    val savedCharge = chargeRepository.save(futureCharge)
    aggravatingFactorsService.replaceAggravatingFactors(savedCharge, existingAggravatingFactorCodes)
    chargeHistoryRepository.save(ChargeHistoryEntity.from(savedCharge, ChangeSource.DPS))
    val eventsToEmit = mutableSetOf(
      EventMetadataCreator.chargeEventMetadata(
        courtCaseHierarchyData.prisonerId,
        courtCaseHierarchyData.courtCaseId!!,
        courtCaseHierarchyData.courtAppearanceUuid.toString(),
        savedCharge.chargeUuid.toString(),
        EventType.CHARGE_UPDATED,
        true,
        courtCaseHierarchyData.isBreach,
      ),
    )
    return RecordResponse(savedCharge, eventsToEmit)
  }

  fun findCharge(appearanceUuid: UUID, chargeUuid: UUID): ChargeEntity? = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(appearanceUuid, chargeUuid) ?: chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid)

  @Transactional
  fun createCharge(createCharge: CreateCharge): RecordResponse<ChargeEntity>? = courtAppearanceRepository.findByAppearanceUuid(createCharge.appearanceUuid!!)?.let {
    if (it.statusId == CourtAppearanceEntityStatus.DELETED) {
      throw AppearanceDeletedException("Court appearance ${createCharge.appearanceUuid} has been deleted and cannot be modified")
    }
    val courtCaseHierarchyData = CourtCaseHierarchyData(
      it.courtCase.prisonerId,
      it.courtCase.caseUniqueIdentifier,
      createCharge.appearanceUuid,
      false,
    )
    createCharge(createCharge, mutableMapOf(), it, courtCaseHierarchyData = courtCaseHierarchyData)
  }

  @Transactional
  fun deleteCharge(
    charge: ChargeEntity,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<ChargeEntity> {
    val changeStatus =
      if (charge.statusId == ChargeEntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    charge.delete(serviceUserService.getUsername())
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    charge.getLiveSentence()?.let {
      eventsToEmit.addAll(
        sentenceService.deleteSentence(
          it,
          charge,
          courtCaseHierarchyData,
        ).eventsToEmit,
      )
    }
    if (changeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.chargeEventMetadata(
          courtCaseHierarchyData.prisonerId,
          courtCaseHierarchyData.courtCaseId!!,
          null,
          charge.chargeUuid.toString(),
          EventType.CHARGE_DELETED,
          false,
          courtCaseHierarchyData.isBreach,
        ),
      )
      chargeHistoryRepository.save(ChargeHistoryEntity.from(charge, ChangeSource.DPS))
    }
    return RecordResponse(charge, eventsToEmit)
  }

  @Transactional
  fun deleteChargeIfOrphan(
    charge: ChargeEntity,
    courtCaseHierarchyData: CourtCaseHierarchyData,
  ): RecordResponse<ChargeEntity> {
    var recordResponse = RecordResponse(charge, mutableSetOf())
    if (charge.appearanceCharges.none { it.appearance!!.statusId == CourtAppearanceEntityStatus.ACTIVE }) {
      recordResponse = deleteCharge(charge, courtCaseHierarchyData)
    }
    return recordResponse
  }

  @Transactional(readOnly = true)
  fun findChargeByUuid(chargeUuid: UUID): Charge? = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid)?.let { Charge.from(it) }

  companion object {
    val replacedWithAnotherOutcomeUuid: UUID = UUID.fromString("68e56c1f-b179-43da-9d00-1272805a7ad3")
  }
}
