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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyPeriodLengthService(private val periodLengthRepository: PeriodLengthRepository, private val periodLengthHistoryRepository: PeriodLengthHistoryRepository, private val serviceUserService: ServiceUserService) {

  fun upsert(createPeriodLengthEntities: Map<NomisPeriodLengthId, PeriodLengthEntity>, sentenceEntity: SentenceEntity): Pair<EntityChangeStatus, Map<NomisPeriodLengthId, PeriodLengthEntity>> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingPeriodLengths = sentenceEntity.periodLengths
    existingPeriodLengths.forEach { existingPeriodLength ->
      val updatedPeriodLength = createPeriodLengthEntities.firstNotNullOfOrNull { if (it.value.periodLengthUuid == existingPeriodLength.periodLengthUuid) it.value else null }
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
    val toAddPeriodLengths = createPeriodLengthEntities.filter { (_, toAddPeriodLength) -> existingPeriodLengths.none { it.periodLengthUuid == toAddPeriodLength.periodLengthUuid } }
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

  fun delete(periodLength: PeriodLengthEntity) {
    periodLength.delete(serviceUserService.getUsername())
    periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(periodLength))
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyPeriodLength {
    val periodLength = getActivePeriodLengthWithSentence(lifetimeUUID)
    val sentenceEntity = periodLength.sentenceEntity ?: throw IllegalStateException("Sentence entity is null for period length $lifetimeUUID")

    return LegacyPeriodLength.from(
      periodLengthEntity = periodLength,
      sentenceTypeClassification = sentenceEntity.sentenceType?.classification,
      sentenceUuid = sentenceEntity.sentenceUuid
    )
  }

  @Transactional(readOnly = true)
  fun getActivePeriodLengthWithSentence(lifetimeUUID: UUID): PeriodLengthEntity {
    return periodLengthRepository.findFirstByPeriodLengthUuidOrderByUpdatedAtDesc(lifetimeUUID)
      ?.takeUnless { it.statusId == EntityStatus.DELETED }
      ?.also { entity ->
        if (entity.sentenceEntity == null) {
          throw IllegalStateException("Period length $lifetimeUUID has no associated sentence")
        }
      }
      ?: throw EntityNotFoundException("No period-length found with UUID $lifetimeUUID")
  }
}
