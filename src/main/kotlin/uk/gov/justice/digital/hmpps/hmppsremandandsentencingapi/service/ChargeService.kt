package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableChargeException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
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
      val chargeEntity = createCharge(charge, emptyMap(), it, courtCaseId!!)
      appearance?.let {
        if (appearance.charges.none { it.chargeUuid == chargeEntity.chargeUuid }) {
          appearance.charges.add(chargeEntity)
        }
      }
      chargeEntity
    }
  }

  @Transactional
  fun createCharge(charge: CreateCharge, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String): ChargeEntity {
    var chargeLegacyData = charge.legacyData
    val outcome = charge.outcomeUuid?.let {
      chargeLegacyData = chargeLegacyData?.copy(nomisOutcomeCode = null, outcomeDescription = null)
      chargeOutcomeRepository.findByOutcomeUuid(it)
    } ?: chargeLegacyData?.nomisOutcomeCode?.let { chargeOutcomeRepository.findByNomisCode(it) }
    val legacyData = chargeLegacyData?.let { objectMapper.valueToTree<JsonNode>(it) }
    val (toCreateCharge, status) = charge.chargeUuid.let { chargeRepository.findByChargeUuid(it) }
      ?.let { chargeEntity ->
        if (chargeEntity.statusId == EntityStatus.EDITED) {
          throw ImmutableChargeException("Cannot edit an already edited charge")
        }
        val compareCharge = ChargeEntity.from(charge, outcome, legacyData, serviceUserService.getUsername())
        if (chargeEntity.isSame(compareCharge)) {
          return@let chargeEntity to EntityChangeStatus.NO_CHANGE
        }
        chargeEntity.statusId = EntityStatus.EDITED
        compareCharge.chargeUuid = UUID.randomUUID()
        charge.chargeUuid = compareCharge.chargeUuid
        compareCharge.supersedingCharge = chargeEntity
        compareCharge.lifetimeChargeUuid = chargeEntity.lifetimeChargeUuid
        compareCharge to EntityChangeStatus.EDITED
      } ?: (ChargeEntity.from(charge, outcome, legacyData, serviceUserService.getUsername()) to EntityChangeStatus.CREATED)
    return chargeRepository.save(toCreateCharge).also {
      if (charge.sentence != null) {
        it.sentences.add(sentenceService.createSentence(charge.sentence, it, sentencesCreated, prisonerId))
      } else {
        it.getActiveSentence()?.let { sentence -> sentenceService.deleteSentence(sentence) }
      }
      if (status == EntityChangeStatus.CREATED) {
        chargeDomainEventService.create(prisonerId, it.lifetimeChargeUuid.toString(), courtCaseId, "DPS")
      } else if (status == EntityChangeStatus.EDITED) {
        chargeDomainEventService.update(prisonerId, it.lifetimeChargeUuid.toString(), courtCaseId, "DPS")
      }
    }
  }

  @Transactional
  fun deleteCharge(chargeUuid: UUID) = chargeRepository.findByChargeUuid(chargeUuid)?.let { deleteCharge(it, it.courtAppearances.firstOrNull()?.courtCase?.prisonerId, it.courtAppearances.firstOrNull()?.courtCase?.caseUniqueIdentifier) }

  @Transactional
  fun deleteCharge(charge: ChargeEntity, prisonerId: String?, courtCaseId: String?) {
    val changeStatus = if (charge.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    charge.statusId = EntityStatus.DELETED
    charge.getActiveSentence()?.let { sentenceService.deleteSentence(it) }
    if (changeStatus == EntityChangeStatus.DELETED) {
      chargeDomainEventService.delete(prisonerId!!, charge.lifetimeChargeUuid.toString(), courtCaseId!!, "DPS")
    }
  }

  @Transactional(readOnly = true)
  fun findChargeByUuid(chargeUuid: UUID): Charge? = chargeRepository.findByChargeUuid(chargeUuid)?.let { Charge.from(it) }
}
