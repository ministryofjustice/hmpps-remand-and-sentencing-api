package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyPeriodLengthService(
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val serviceUserService: ServiceUserService,
  private val sentenceRepository: SentenceRepository,
) {
  @Transactional
  fun create(periodLength: LegacyCreatePeriodLength): LegacyPeriodLengthCreatedResponse {
    val sentenceEntities = sentenceRepository.findBySentenceUuid(periodLength.sentenceUuid)
    val firstSentenceEntity = sentenceEntities.firstOrNull()
      ?: throw EntityNotFoundException("No sentence found with UUID ${periodLength.sentenceUuid}")

    val periodLengthUuid = UUID.randomUUID()
    sentenceEntities.forEach { sentenceEntity ->
      val periodLengthEntity = PeriodLengthEntity.from(
        periodLengthUuid = periodLengthUuid,
        periodLength = periodLength,
        sentenceEntity = sentenceEntity,
        createdBy = getPerformedByUsername(periodLength),
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

  @Transactional
  fun update(periodLengthUuid: UUID, periodLengthUpdate: LegacyCreatePeriodLength): LegacyPeriodLengthCreatedResponse? {
    val existingPeriodLengths = periodLengthRepository.findByPeriodLengthUuid(periodLengthUuid)
      .filter { it.sentenceEntity != null }
      .takeIf { it.isNotEmpty() }
      ?: throw EntityNotFoundException("No sentence related period length found with UUID $periodLengthUuid")

    val sentenceEntities = sentenceRepository.findBySentenceUuid(periodLengthUpdate.sentenceUuid)
      .takeIf { it.isNotEmpty() }
      ?: throw EntityNotFoundException("No sentence found with UUID ${periodLengthUpdate.sentenceUuid}")

    val username = getPerformedByUsername(periodLengthUpdate)
    var changesMade = false

    existingPeriodLengths.forEach { existingEntity ->
      val originalCopy = existingEntity.copy()

      existingEntity.updateFrom(
        periodLength = periodLengthUpdate,
        sentenceEntity = existingEntity.sentenceEntity!!,
        username = username,
      )

      if (!originalCopy.isSame(existingEntity)) {
        periodLengthRepository.save(existingEntity)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(existingEntity))
        changesMade = true
      }
    }

    if (!changesMade) {
      return null
    }

    val firstSentenceEntity = sentenceEntities.first()
    val appearance = firstSentenceEntity.charge.appearanceCharges.firstOrNull()?.appearance
      ?: throw EntityNotFoundException("No appearance found for sentence ${firstSentenceEntity.sentenceUuid}")

    return LegacyPeriodLengthCreatedResponse(
      periodLengthUuid = periodLengthUuid,
      sentenceUuid = periodLengthUpdate.sentenceUuid,
      chargeUuid = firstSentenceEntity.charge.chargeUuid,
      appearanceUuid = appearance.appearanceUuid,
      courtCaseId = appearance.courtCase.id.toString(),
      prisonerId = appearance.courtCase.prisonerId,
    )
  }

  private fun getPerformedByUsername(periodLength: LegacyCreatePeriodLength): String = periodLength.performedByUser ?: serviceUserService.getUsername()

  fun delete(periodLength: PeriodLengthEntity, performedByUser: String) {
    periodLength.delete(performedByUser)
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
    ?.takeUnless { it.statusId == PeriodLengthEntityStatus.DELETED }?.also { entity ->
      if (entity.sentenceEntity == null) {
        throw IllegalStateException("Period length $periodLengthUuid has no associated sentence")
      }
    } ?: throw EntityNotFoundException("No period-length found with UUID $periodLengthUuid")

  @Transactional
  fun deletePeriodLengthWithSentence(periodLengthUuid: UUID, performedByUser: String?): LegacyPeriodLength? = periodLengthRepository.findByPeriodLengthUuid(periodLengthUuid)
    .filter { it.statusId != PeriodLengthEntityStatus.DELETED && it.sentenceEntity != null }.map { periodLength ->
      delete(periodLength, performedByUser ?: serviceUserService.getUsername())
      LegacyPeriodLength.from(periodLength, periodLength.sentenceEntity!!)
    }.firstOrNull()
}
