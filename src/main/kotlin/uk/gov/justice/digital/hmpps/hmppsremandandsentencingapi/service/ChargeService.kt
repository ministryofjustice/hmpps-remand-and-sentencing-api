package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
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
) {

  private fun createChargeEntity(
    charge: CreateCharge,
    sentencesCreated: MutableMap<UUID, SentenceEntity>,
    prisonerId: String,
    courtCaseId: String,
    courtAppearanceId: String,
    supersedingCharge: ChargeEntity?,
  ): RecordResponse<ChargeEntity> {
    val chargeToSupersede: ChargeEntity? = supersedingCharge ?: charge.replacingChargeUuid?.let { chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(it) }
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
    chargeHistoryRepository.save(ChargeHistoryEntity.from(savedCharge))
    val eventsToEmit = mutableSetOf(
      EventMetadataCreator.chargeEventMetadata(
        prisonerId,
        courtCaseId,
        null,
        savedCharge.chargeUuid.toString(),
        EventType.CHARGE_INSERTED,
      ),
    )
    charge.sentence?.let { createSentence ->
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(
        createSentence,
        savedCharge,
        sentencesCreated,
        prisonerId,
        courtCaseId,
        false,
        courtAppearanceId,
      )
      savedCharge.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    }
    return RecordResponse(savedCharge, eventsToEmit)
  }

  private fun updateChargeEntity(
    charge: CreateCharge,
    sentencesCreated: MutableMap<UUID, SentenceEntity>,
    prisonerId: String,
    courtCaseId: String,
    existingCharge: ChargeEntity,
    courtAppearance: CourtAppearanceEntity,
    courtAppearanceDateChanged: Boolean,
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
        existingCharge.updateFrom(replacedWithAnotherOutcome, serviceUserService.getUsername(), charge.prisonId)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge))

        val appearanceChargeEntity = AppearanceChargeEntity(
          courtAppearance,
          existingCharge,
          serviceUserService.getUsername(),
          charge.prisonId,
        )
        courtAppearance.appearanceCharges.add(appearanceChargeEntity)
        existingCharge.appearanceCharges.add(appearanceChargeEntity)
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity))

        chargeChanges.add(EntityChangeStatus.EDITED to existingCharge)

        val newChargeRecord = chargeRepository.save(compareCharge.copyFromReplacedCharge(existingCharge))
        chargeHistoryRepository.save(ChargeHistoryEntity.from(newChargeRecord))
        activeRecord = newChargeRecord
        chargeChanges.add(EntityChangeStatus.CREATED to newChargeRecord)
        eventsToEmit.addAll(
          sentenceService.moveSentencesToNewCharge(
            existingCharge,
            newChargeRecord,
            prisonerId,
            courtCaseId,
            courtAppearance.appearanceUuid.toString(),
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
              ),
            )
            appearanceCharge.charge = null
            appearanceCharge.appearance = null
          }
        compareCharge.appearanceCharges.removeAll { it.appearance == null }
        activeRecord = chargeRepository.save(compareCharge)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(activeRecord))
        chargeChanges.add(EntityChangeStatus.EDITED to activeRecord)
      } else {
        existingCharge.updateFrom(compareCharge)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge))
        chargeChanges.add(EntityChangeStatus.EDITED to existingCharge)
      }
    }
    if (charge.sentence != null) {
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(
        charge.sentence,
        activeRecord,
        sentencesCreated,
        prisonerId,
        courtCaseId,
        courtAppearanceDateChanged,
        courtAppearance.appearanceUuid.toString(),
      )
      activeRecord.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    } else {
      activeRecord.getLiveSentence()?.let { sentenceEntity ->
        eventsToEmit.addAll(
          sentenceService.deleteSentence(
            sentenceEntity,
            activeRecord,
            prisonerId,
            courtCaseId,
            courtAppearance.appearanceUuid.toString(),
          ).eventsToEmit,
        )
      }
    }
    chargeChanges.forEach { (chargeChangeStatus, record) ->
      if (chargeChangeStatus == EntityChangeStatus.EDITED) {
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            prisonerId,
            courtCaseId,
            courtAppearance.appearanceUuid.toString(),
            record.chargeUuid.toString(),
            EventType.CHARGE_UPDATED,
          ),
        )
      } else if (chargeChangeStatus == EntityChangeStatus.CREATED) {
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            prisonerId,
            courtCaseId,
            courtAppearance.appearanceUuid.toString(),
            record.chargeUuid.toString(),
            EventType.CHARGE_INSERTED,
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
    prisonerId: String,
    courtCaseId: String,
    courtAppearance: CourtAppearanceEntity,
    courtAppearanceDateChanged: Boolean,
    supersedingCharge: ChargeEntity? = null,
  ): RecordResponse<ChargeEntity> {
    val existingCharge = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(charge.chargeUuid)
    val charge = if (existingCharge != null) {
      updateChargeEntity(
        charge,
        sentencesCreated,
        prisonerId,
        courtCaseId,
        existingCharge,
        courtAppearance,
        courtAppearanceDateChanged,
      )
    } else {
      createChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId, courtAppearance.appearanceUuid.toString(), supersedingCharge)
    }
    return charge
  }

  @Transactional
  fun deleteCharge(
    charge: ChargeEntity,
    prisonerId: String?,
    courtCaseId: String?,
    courtAppearanceId: String,
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
          prisonerId!!,
          courtCaseId!!,
          courtAppearanceId,
        ).eventsToEmit,
      )
    }
    if (changeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.chargeEventMetadata(
          prisonerId!!,
          courtCaseId!!,
          null,
          charge.chargeUuid.toString(),
          EventType.CHARGE_DELETED,
        ),
      )
      chargeHistoryRepository.save(ChargeHistoryEntity.from(charge))
    }
    return RecordResponse(charge, eventsToEmit)
  }

  @Transactional
  fun deleteChargeIfOrphan(
    charge: ChargeEntity,
    prisonerId: String,
    courtCaseId: String,
    courtAppearanceId: String,
  ): RecordResponse<ChargeEntity> {
    var recordResponse = RecordResponse(charge, mutableSetOf())
    if (charge.appearanceCharges.none { it.appearance!!.statusId == CourtAppearanceEntityStatus.ACTIVE }) {
      recordResponse = deleteCharge(charge, prisonerId, courtCaseId, courtAppearanceId)
    }
    return recordResponse
  }

  @Transactional(readOnly = true)
  fun findChargeByUuid(chargeUuid: UUID): Charge? = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid)?.let { Charge.from(it) }

  companion object {
    val replacedWithAnotherOutcomeUuid: UUID = UUID.fromString("68e56c1f-b179-43da-9d00-1272805a7ad3")
  }
}
