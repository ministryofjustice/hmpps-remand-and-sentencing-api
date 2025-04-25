package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import java.util.function.Consumer

@Service
class PeriodLengthService(private val periodLengthRepository: PeriodLengthRepository, private val periodLengthHistoryRepository: PeriodLengthHistoryRepository, private val serviceUserService: ServiceUserService) {

  fun upsert(createPeriodLengthEntities: List<PeriodLengthEntity>, existingPeriodLengths: MutableSet<PeriodLengthEntity>, onCreateConsumer: Consumer<PeriodLengthEntity>): EntityChangeStatus {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    existingPeriodLengths.forEach { existingPeriodLength ->
      val updatedPeriodLength = createPeriodLengthEntities.firstOrNull { it.periodLengthUuid == existingPeriodLength.periodLengthUuid }
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

      existingPeriodLength
    }
    val toAddPeriodLengths = createPeriodLengthEntities.filter { toAddPeriodLength -> existingPeriodLengths.none { it.periodLengthUuid == toAddPeriodLength.periodLengthUuid } }
      .map {
        onCreateConsumer.accept(it)
        val savedPeriodLength = periodLengthRepository.save(it)
        periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(savedPeriodLength))
        EntityChangeStatus.CREATED
        savedPeriodLength
      }
    existingPeriodLengths.addAll(toAddPeriodLengths)
    return entityChangeStatus
  }
}
