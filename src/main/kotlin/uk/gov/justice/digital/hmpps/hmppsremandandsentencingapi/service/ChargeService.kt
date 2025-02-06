package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableChargeException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.util.UUID

@Service
class ChargeService(private val chargeRepository: ChargeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val sentenceService: SentenceService, private val objectMapper: ObjectMapper, private val serviceUserService: ServiceUserService) {

  private fun createChargeEntity(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String): RecordResponse<ChargeEntity> {
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    charge.legacyData = chargeLegacyData
    val savedCharge = chargeRepository.save(ChargeEntity.from(charge, chargeOutcome, serviceUserService.getUsername()))
    val eventsToEmit = mutableListOf(
      EventMetadataCreator.chargeEventMetadata(
        prisonerId,
        courtCaseId,
        null,
        savedCharge.lifetimeChargeUuid.toString(),
        EventType.CHARGE_INSERTED,
      ),
    )
    charge.sentence?.let { createSentence ->
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(createSentence, savedCharge, sentencesCreated, prisonerId)
      savedCharge.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    }
    return RecordResponse(savedCharge, eventsToEmit)
  }

  private fun updateChargeEntity(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, existingCharge: ChargeEntity, courtAppearance: CourtAppearanceEntity): RecordResponse<ChargeEntity> {
    if (existingCharge.statusId == EntityStatus.EDITED) {
      throw ImmutableChargeException("Cannot edit an already edited charge")
    }
    val chargeChanges = mutableListOf<Pair<EntityChangeStatus, ChargeEntity>>()
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    charge.legacyData = chargeLegacyData
    val compareCharge = existingCharge.copyFrom(charge, chargeOutcome, serviceUserService.getUsername())
    var activeRecord = existingCharge
    val eventsToEmit: MutableList<EventMetadata> = mutableListOf()
    if (!existingCharge.isSame(compareCharge)) {
      var chargeChangeStatus = EntityChangeStatus.EDITED
      if (existingCharge.offenceCode != compareCharge.offenceCode) {
        // update existing charge with replaced by charge outcome
        val replacedWithAnotherOutcome = chargeOutcomeRepository.findByOutcomeUuid(replacedWithAnotherOutcomeUuid)
        val updatedExistingCharge = chargeRepository.save(existingCharge.copyFrom(replacedWithAnotherOutcome, serviceUserService.getUsername()))
        courtAppearance.charges.add(updatedExistingCharge)
        updatedExistingCharge.courtAppearances.add(courtAppearance)
        chargeChanges.add(EntityChangeStatus.EDITED to updatedExistingCharge)
        chargeChangeStatus = EntityChangeStatus.CREATED
        compareCharge.supersedingCharge = updatedExistingCharge
        compareCharge.lifetimeChargeUuid = UUID.randomUUID()
      }
      if (existingCharge.hasTwoOrMoreActiveCourtAppearance(courtAppearance)) {
        existingCharge.courtAppearances.remove(courtAppearance)
        courtAppearance.charges.remove(existingCharge)
      } else {
        existingCharge.statusId = EntityStatus.EDITED
      }
      activeRecord = chargeRepository.save(compareCharge)
      chargeChanges.add(chargeChangeStatus to activeRecord)
    }
    if (charge.sentence != null) {
      val (sentence, sentenceEventsToEmit) = sentenceService.createSentence(charge.sentence, activeRecord, sentencesCreated, prisonerId)
      activeRecord.sentences.add(sentence)
      eventsToEmit.addAll(sentenceEventsToEmit)
    } else {
      activeRecord.getActiveSentence()?.let { sentenceEntity -> eventsToEmit.addAll(sentenceService.deleteSentence(sentenceEntity, activeRecord, prisonerId).eventsToEmit) }
    }
    chargeChanges.forEach { (chargeChangeStatus, record) ->
      if (chargeChangeStatus == EntityChangeStatus.EDITED) {
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            prisonerId,
            courtCaseId,
            courtAppearance.lifetimeUuid.toString(),
            record.lifetimeChargeUuid.toString(),
            EventType.CHARGE_UPDATED,
          ),
        )
      } else if (chargeChangeStatus == EntityChangeStatus.CREATED) {
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            prisonerId,
            courtCaseId,
            courtAppearance.lifetimeUuid.toString(),
            record.lifetimeChargeUuid.toString(),
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
  fun createCharge(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearance: CourtAppearanceEntity): RecordResponse<ChargeEntity> {
    val existingCharge = chargeRepository.findByChargeUuid(charge.chargeUuid)
    val charge = if (existingCharge != null) {
      updateChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId, existingCharge, courtAppearance)
    } else {
      createChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId)
    }
    return charge
  }

  @Transactional
  fun deleteCharge(charge: ChargeEntity, prisonerId: String?, courtCaseId: String?): RecordResponse<ChargeEntity> {
    val changeStatus = if (charge.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    charge.statusId = EntityStatus.DELETED
    val eventsToEmit: MutableList<EventMetadata> = mutableListOf()
    charge.getActiveSentence()?.let { eventsToEmit.addAll(sentenceService.deleteSentence(it, charge, prisonerId!!).eventsToEmit) }
    if (changeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.chargeEventMetadata(
          prisonerId!!,
          courtCaseId!!,
          null,
          charge.lifetimeChargeUuid.toString(),
          EventType.CHARGE_DELETED,
        ),
      )
    }
    return RecordResponse(charge, eventsToEmit)
  }

  @Transactional
  fun deleteChargeIfOrphan(charge: ChargeEntity, prisonerId: String, courtCaseId: String): RecordResponse<ChargeEntity> {
    var recordResponse = RecordResponse(charge, mutableListOf())
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
