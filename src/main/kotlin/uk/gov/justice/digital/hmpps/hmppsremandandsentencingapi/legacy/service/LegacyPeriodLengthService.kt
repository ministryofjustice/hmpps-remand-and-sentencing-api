package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyPeriodLengthService(
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
  private val sentenceRepository: SentenceRepository,
) {
  fun create(periodLength: LegacyCreatePeriodLength): LegacyPeriodLengthCreatedResponse {
    val sentenceEntities = sentenceRepository.findBySentenceUuid(periodLength.sentenceUuid)
    val firstSentenceEntity = sentenceEntities.firstOrNull()
      ?: throw EntityNotFoundException("No sentence found with UUID ${periodLength.sentenceUuid}")
    val isManyCharges = firstSentenceEntity.charge.appearanceCharges.size > 1
    val periodLengthUuid = UUID.randomUUID()
    sentenceEntities.forEach { sentenceEntity ->
      val periodLengthEntity = PeriodLengthEntity.from(
        periodLengthUuid,
        periodLength,
        sentenceEntity,
        serviceUserService.getUsername(),
        isManyCharges,
      )
      val savedPeriodLength = periodLengthRepository.save(periodLengthEntity)
      periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(savedPeriodLength))
    }
    val appearance = firstSentenceEntity.charge.appearanceCharges.firstOrNull()?.appearance
      ?: throw EntityNotFoundException("No appearance found for sentence ${firstSentenceEntity.sentenceUuid}")

    return LegacyPeriodLengthCreatedResponse(
      periodLengthUuid = periodLengthUuid,
      sentenceUuid = periodLength.sentenceUuid,
      chargeUuid = firstSentenceEntity.charge.chargeUuid,
      appearanceUuid = appearance.appearanceUuid,
      courtCaseId = appearance.courtCase.id.toString(),
      prisonerId = appearance.courtCase.prisonerId,
    )
  }

  fun upsert(
    createPeriodLengthEntities: Map<NomisPeriodLengthId, PeriodLengthEntity>,
    sentenceEntity: SentenceEntity,
  ): Pair<EntityChangeStatus, Map<NomisPeriodLengthId, PeriodLengthEntity>> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingPeriodLengths = sentenceEntity.periodLengths
    existingPeriodLengths.forEach { existingPeriodLength ->
      val updatedPeriodLength =
        createPeriodLengthEntities.firstNotNullOfOrNull { if (it.value.periodLengthUuid == existingPeriodLength.periodLengthUuid) it.value else null }
      if (updatedPeriodLength != null) {
        if (!existingPeriodLength.isSame(updatedPeriodLength)) {
          existingPeriodLength.updateFrom(updatedPeriodLength, serviceUserService.getUsername())
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingPeriodLength))
          entityChangeStatus = EntityChangeStatus.EDITED
        }
      } else {
        existingPeriodLength.delete(serviceUserService.getUsername())
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingPeriodLength))
        EntityChangeStatus.DELETED
      }
    }
    val toAddPeriodLengths =
      createPeriodLengthEntities.filter { (_, toAddPeriodLength) -> existingPeriodLengths.none { it.periodLengthUuid == toAddPeriodLength.periodLengthUuid } }
        .mapValues { (_, toAddPeriodLength) ->
          toAddPeriodLength.sentenceEntity = sentenceEntity
          val savedPeriodLength = periodLengthRepository.save(toAddPeriodLength)
          periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(savedPeriodLength))
          EntityChangeStatus.CREATED
          savedPeriodLength
        }
    existingPeriodLengths.addAll(toAddPeriodLengths.values)
    return entityChangeStatus to toAddPeriodLengths
  }

  private fun delete(periodLength: PeriodLengthEntity) {
    periodLength.delete(serviceUserService.getUsername())
    periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength))
  }

  @Transactional(readOnly = true)
  fun get(periodLengthUuid: UUID): LegacyPeriodLength {
    val periodLength = getActivePeriodLengthWithSentence(periodLengthUuid)

    return LegacyPeriodLength.from(
      periodLengthEntity = periodLength,
      sentenceEntity = periodLength.sentenceEntity!!,
    )
  }

  @Transactional(readOnly = true)
  fun getActivePeriodLengthWithSentence(periodLengthUuid: UUID): PeriodLengthEntity = periodLengthRepository.findFirstByPeriodLengthUuidOrderByUpdatedAtDesc(periodLengthUuid)
    ?.takeUnless { it.statusId == EntityStatus.DELETED }?.also { entity ->
      if (entity.sentenceEntity == null) {
        throw IllegalStateException("Period length $periodLengthUuid has no associated sentence")
      }
    } ?: throw EntityNotFoundException("No period-length found with UUID $periodLengthUuid")

  @Transactional
  fun deletePeriodLengthWithSentence(periodLengthUuid: UUID): LegacyPeriodLength? = periodLengthRepository.findByPeriodLengthUuid(periodLengthUuid)
    .filter { it.statusId != EntityStatus.DELETED && it.sentenceEntity != null }.map { periodLength ->
      delete(periodLength)
      LegacyPeriodLength.from(periodLength, periodLength.sentenceEntity!!)
    }.firstOrNull()
}
