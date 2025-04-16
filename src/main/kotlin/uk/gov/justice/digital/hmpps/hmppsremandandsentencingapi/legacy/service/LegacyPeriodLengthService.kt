package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class LegacyPeriodLengthService(private val periodLengthRepository: PeriodLengthRepository, private val periodLengthHistoryRepository: PeriodLengthHistoryRepository, private val serviceUserService: ServiceUserService) {

  fun upsert(createPeriodLengthEntities: Map<NomisPeriodLengthId, PeriodLengthEntity>, sentenceEntity: SentenceEntity): Pair<EntityChangeStatus, Map<NomisPeriodLengthId, PeriodLengthEntity>> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingPeriodLengths = sentenceEntity.periodLengths.filter { setOf(EntityStatus.ACTIVE, EntityStatus.MANY_CHARGES_DATA_FIX, EntityStatus.INACTIVE).contains(it.statusId) }.toMutableList()
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
}
