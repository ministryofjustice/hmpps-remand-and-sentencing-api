package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableChargeException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.util.UUID

@Service
class ChargeService(private val chargeRepository: ChargeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val sentenceService: SentenceService, private val chargeDomainEventService: ChargeDomainEventService, private val objectMapper: ObjectMapper, private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceRepository: CourtAppearanceRepository, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun createCharge(charge: CreateCharge): ChargeEntity? {
    val appearance = charge.appearanceUuid?.let { courtAppearanceRepository.findByAppearanceUuid(it) }
    var prisonerId = appearance?.courtCase?.prisonerId
    var courtCaseId: String? = appearance?.courtCase?.caseUniqueIdentifier
    if (appearance == null) {
      courtCaseRepository.findFirstByAppearancesChargesChargeUuid(charge.chargeUuid)?.also {
        prisonerId = it.prisonerId
        courtCaseId = it.caseUniqueIdentifier
      }
    }
    return prisonerId?.let {
      val chargeEntity = createCharge(charge, emptyMap(), it, courtCaseId!!, appearance!!)
      if (appearance.charges.none { it.chargeUuid == chargeEntity.chargeUuid }) {
        appearance.charges.add(chargeEntity)
      }
      chargeEntity
    }
  }

  private fun createChargeEntity(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String): ChargeEntity {
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    val legacyData = chargeLegacyData?.let { objectMapper.valueToTree<JsonNode>(it) }
    val savedCharge = chargeRepository.save(ChargeEntity.from(charge, chargeOutcome, legacyData, serviceUserService.getUsername()))
    charge.sentence?.let { createSentence ->
      savedCharge.sentences.add(sentenceService.createSentence(createSentence, savedCharge, sentencesCreated, prisonerId))
    }
    chargeDomainEventService.create(prisonerId, savedCharge.lifetimeChargeUuid.toString(), courtCaseId, EventSource.DPS)
    return savedCharge
  }

  private fun updateChargeEntity(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, existingCharge: ChargeEntity, courtAppearance: CourtAppearanceEntity): ChargeEntity {
    if (existingCharge.statusId == EntityStatus.EDITED) {
      throw ImmutableChargeException("Cannot edit an already edited charge")
    }
    var chargeChangeStatus = EntityChangeStatus.NO_CHANGE
    val (chargeLegacyData, chargeOutcome) = getChargeOutcome(charge)
    val legacyData = chargeLegacyData?.let { objectMapper.valueToTree<JsonNode>(it) }
    val compareCharge = existingCharge.copyFrom(charge, chargeOutcome, legacyData, serviceUserService.getUsername())
    var activeRecord = existingCharge

    if (!existingCharge.isSame(compareCharge)) {
      if (existingCharge.hasTwoOrMoreActiveCourtAppearance(courtAppearance)) {
        existingCharge.courtAppearances.remove(courtAppearance)
        courtAppearance.charges.remove(existingCharge)
      } else {
        existingCharge.statusId = EntityStatus.EDITED
      }
      activeRecord = chargeRepository.save(compareCharge)
      chargeChangeStatus = EntityChangeStatus.EDITED
    }
    if (charge.sentence != null) {
      activeRecord.sentences.add(sentenceService.createSentence(charge.sentence, activeRecord, sentencesCreated, prisonerId))
    } else {
      activeRecord.getActiveSentence()?.let { sentenceEntity -> sentenceService.deleteSentence(sentenceEntity) }
    }
    if (chargeChangeStatus == EntityChangeStatus.EDITED) {
      chargeDomainEventService.update(prisonerId, activeRecord.lifetimeChargeUuid.toString(), courtAppearance.lifetimeUuid.toString(), courtCaseId, EventSource.DPS)
    }
    return activeRecord
  }

  private fun getChargeOutcome(charge: CreateCharge): Pair<ChargeLegacyData?, ChargeOutcomeEntity?> {
    var chargeLegacyData = charge.legacyData
    val chargeOutcome = charge.outcomeUuid?.let {
      chargeLegacyData = chargeLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null)
      chargeOutcomeRepository.findByOutcomeUuid(it)
    } ?: chargeLegacyData?.nomisOutcomeCode?.let { chargeOutcomeRepository.findByNomisCode(it) }
    return chargeLegacyData to chargeOutcome
  }

  @Transactional
  fun createCharge(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearance: CourtAppearanceEntity): ChargeEntity {
    val existingCharge = chargeRepository.findByChargeUuid(charge.chargeUuid)
    val charge = if (existingCharge != null) {
      updateChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId, existingCharge, courtAppearance)
    } else {
      createChargeEntity(charge, sentencesCreated, prisonerId, courtCaseId)
    }
    return charge
  }

  @Transactional
  fun deleteCharge(chargeUuid: UUID) = chargeRepository.findByChargeUuid(chargeUuid)?.let { deleteCharge(it, it.courtAppearances.firstOrNull()?.courtCase?.prisonerId, it.courtAppearances.firstOrNull()?.courtCase?.caseUniqueIdentifier) }

  @Transactional
  fun deleteCharge(charge: ChargeEntity, prisonerId: String?, courtCaseId: String?) {
    val changeStatus = if (charge.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    charge.statusId = EntityStatus.DELETED
    charge.getActiveSentence()?.let { sentenceService.deleteSentence(it) }
    if (changeStatus == EntityChangeStatus.DELETED) {
      chargeDomainEventService.delete(prisonerId!!, charge.lifetimeChargeUuid.toString(), courtCaseId!!, EventSource.DPS)
    }
  }

  @Transactional
  fun deleteChargeIfOrphan(charge: ChargeEntity, prisonerId: String, courtCaseId: String) {
    if (charge.courtAppearances.none { it.statusId == EntityStatus.ACTIVE }) {
      deleteCharge(charge, prisonerId, courtCaseId)
    }
  }

  @Transactional(readOnly = true)
  fun findChargeByUuid(chargeUuid: UUID): Charge? = chargeRepository.findByChargeUuid(chargeUuid)?.let { Charge.from(it) }
}
