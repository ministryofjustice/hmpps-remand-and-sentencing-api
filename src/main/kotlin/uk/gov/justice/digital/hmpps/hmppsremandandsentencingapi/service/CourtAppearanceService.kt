package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtAppearanceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
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
  fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): CourtAppearanceEntity {
    val appearanceOutcome = appearanceOutcomeRepository.findByOutcomeName(courtAppearance.outcome) ?: appearanceOutcomeRepository.save(AppearanceOutcomeEntity(outcomeName = courtAppearance.outcome))
    val sentencesCreated = mutableMapOf<String, SentenceEntity>()
    val charges = courtAppearance.charges.sortedWith(this::chargesByConsecutiveToLast).map {
      val charge = chargeService.createCharge(it, sentencesCreated)
      charge.getActiveSentence()?.let { sentence -> sentencesCreated.put(sentence.chargeNumber, sentence) }
      charge
    }.toMutableSet()
    val toCreateAppearance = courtAppearance.appearanceUuid?.let { courtAppearanceRepository.findByAppearanceUuid(it) }
      ?.let { courtAppearanceEntity ->
        if (courtAppearanceEntity.statusId == EntityStatus.EDITED) {
          throw ImmutableCourtAppearanceException("Cannot edit an already edited court appearance")
        }
        val compareAppearance = CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges)
        if (courtAppearanceEntity.isSame(compareAppearance)) {
          val toDeleteCharges = courtAppearanceEntity.charges.filter { existingCharge -> courtAppearance.charges.none { it.chargeUuid == existingCharge.chargeUuid } }
          toDeleteCharges.forEach { chargeService.deleteCharge(it) }

          courtAppearanceEntity.charges.addAll(charges)
          return@let courtAppearanceEntity
        }
        courtAppearanceEntity.statusId = EntityStatus.EDITED
        compareAppearance.previousAppearance = courtAppearanceEntity
        compareAppearance.appearanceUuid = UUID.randomUUID()
        compareAppearance
      } ?: CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges)

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
    return createdCourtAppearance
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
  fun deleteCourtAppearance(courtAppearanceEntity: CourtAppearanceEntity) {
    courtAppearanceEntity.statusId = EntityStatus.DELETED
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): CourtAppearance? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { CourtAppearance.from(it) }
}
