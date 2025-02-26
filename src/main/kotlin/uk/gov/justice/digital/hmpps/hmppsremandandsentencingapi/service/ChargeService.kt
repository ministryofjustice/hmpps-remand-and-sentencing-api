package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.util.UUID

@Service
class ChargeService(private val chargeRepository: ChargeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val sentenceService: SentenceService, private val serviceUserService: ServiceUserService, private val chargeHistoryRepository: ChargeHistoryRepository) {

  private fun createChargeEntity(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String): RecordResponse<ChargeEntity> {
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    charge.legacyData = chargeLegacyData
    val savedCharge = chargeRepository.save(ChargeEntity.from(charge, chargeOutcome, serviceUserService.getUsername()))
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
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(createSentence, savedCharge, sentencesCreated, prisonerId, courtCaseId, false)
      savedCharge.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    }
    return RecordResponse(savedCharge, eventsToEmit)
  }

  private fun updateChargeEntity(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, existingCharge: ChargeEntity, courtAppearance: CourtAppearanceEntity, courtAppearanceDateChanged: Boolean): RecordResponse<ChargeEntity> {
    val chargeChanges = mutableListOf<Pair<EntityChangeStatus, ChargeEntity>>()
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    charge.legacyData = chargeLegacyData
    var compareCharge = existingCharge.copyFrom(charge, chargeOutcome, serviceUserService.getUsername())
    var activeRecord = existingCharge
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    if (!existingCharge.isSame(compareCharge)) {
      if (existingCharge.offenceCode != compareCharge.offenceCode) {
        val replacedWithAnotherOutcome = chargeOutcomeRepository.findByOutcomeUuid(replacedWithAnotherOutcomeUuid)
        existingCharge.updateFrom(replacedWithAnotherOutcome, serviceUserService.getUsername(), charge.prisonId)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge))
        courtAppearance.charges.add(existingCharge)
        existingCharge.courtAppearances.add(courtAppearance)
        chargeChanges.add(EntityChangeStatus.EDITED to existingCharge)
        val newChargeRecord = chargeRepository.save(compareCharge.copyFromReplacedCharge(existingCharge))
        chargeHistoryRepository.save(ChargeHistoryEntity.from(newChargeRecord))
        activeRecord = newChargeRecord
        chargeChanges.add(EntityChangeStatus.CREATED to newChargeRecord)
      } else if (existingCharge.hasTwoOrMoreActiveCourtAppearance(courtAppearance)) {
        existingCharge.courtAppearances.remove(courtAppearance)
        courtAppearance.charges.remove(existingCharge)
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
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(charge.sentence, activeRecord, sentencesCreated, prisonerId, courtCaseId, courtAppearanceDateChanged)
      activeRecord.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    } else {
      activeRecord.getActiveSentence()?.let { sentenceEntity -> eventsToEmit.addAll(sentenceService.deleteSentence(sentenceEntity, activeRecord, prisonerId, courtCaseId).eventsToEmit) }
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
      chargeLegacyData = chargeLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null)
      chargeOutcomeRepository.findByOutcomeUuid(it)
    } ?: chargeLegacyData?.nomisOutcomeCode?.let { chargeOutcomeRepository.findByNomisCode(it) }
    return chargeLegacyData to chargeOutcome
  }

  @Transactional
  fun createCharge(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearance: CourtAppearanceEntity, courtAppearanceDateChanged: Boolean): RecordResponse<ChargeEntity> {
    val existingCharge = chargeRepository.findByChargeUuid(charge.chargeUuid)
    val charge = if (existingCharge != null) {
      updateChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId, existingCharge, courtAppearance, courtAppearanceDateChanged)
    } else {
      createChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId)
    }
    return charge
  }

  @Transactional
  fun deleteCharge(charge: ChargeEntity, prisonerId: String?, courtCaseId: String?): RecordResponse<ChargeEntity> {
    val changeStatus = if (charge.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    charge.statusId = EntityStatus.DELETED
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    charge.getActiveSentence()?.let { eventsToEmit.addAll(sentenceService.deleteSentence(it, charge, prisonerId!!, courtCaseId!!).eventsToEmit) }
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
    }
    return RecordResponse(charge, eventsToEmit)
  }

  @Transactional
  fun deleteChargeIfOrphan(charge: ChargeEntity, prisonerId: String, courtCaseId: String): RecordResponse<ChargeEntity> {
    var recordResponse = RecordResponse(charge, mutableSetOf())
    if (charge.courtAppearances.none { it.statusId == EntityStatus.ACTIVE }) {
      recordResponse = deleteCharge(charge, prisonerId, courtCaseId)
    }
    return recordResponse
  }

  @Transactional(readOnly = true)
  fun findChargeByUuid(chargeUuid: UUID): Charge? = chargeRepository.findByChargeUuid(chargeUuid)?.let { Charge.from(it) }

  companion object {
    val replacedWithAnotherOutcomeUuid: UUID = UUID.fromString("68e56c1f-b179-43da-9d00-1272805a7ad3")
  }
}
