package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.domain.AppearanceTypeCourtAppearanceSubtype
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyAppearanceTypeService
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class NextCourtAppearanceService(private val nextCourtAppearanceRepository: NextCourtAppearanceRepository, private val legacyAppearanceTypeService: LegacyAppearanceTypeService, private val courtAppearanceRepository: CourtAppearanceRepository, private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository) {

  fun handleNextCourtAppearance(courtAppearance: CourtAppearanceEntity, resultCode: String?, performedByUser: String, appearanceDateTime: LocalDateTime, nextCourtAppearanceFunction: (AppearanceTypeCourtAppearanceSubtype) -> NextCourtAppearanceEntity) {
    nextCourtAppearanceRepository.findFirstByFutureSkeletonAppearance(courtAppearance)?.let { existingNextCourtAppearance ->
      val appearanceTypeCourtAppearanceSubtype = legacyAppearanceTypeService.getAppearanceType(resultCode)
      val toUpdate = nextCourtAppearanceFunction(appearanceTypeCourtAppearanceSubtype)
      existingNextCourtAppearance.updateFrom(toUpdate)
    } ?: handleMatchingNextCourtAppearance(courtAppearance, resultCode, performedByUser, appearanceDateTime, nextCourtAppearanceFunction)
  }

  private fun handleMatchingNextCourtAppearance(courtAppearance: CourtAppearanceEntity, resultCode: String?, performedByUser: String, appearanceDateTime: LocalDateTime, nextCourtAppearanceFunction: (AppearanceTypeCourtAppearanceSubtype) -> NextCourtAppearanceEntity) {
    courtAppearance.takeIf { it.statusId == CourtAppearanceEntityStatus.FUTURE }?.let { getMatchedNextCourtAppearanceOrLatest(it.courtCase, appearanceDateTime, courtAppearance.id) }?.let { matchedCourtAppearance ->
      val appearanceTypeCourtAppearanceSubtype = legacyAppearanceTypeService.getAppearanceType(resultCode)
      matchedCourtAppearance.nextCourtAppearance?.let { matchedNextCourtAppearance ->
        val toUpdate = nextCourtAppearanceFunction(appearanceTypeCourtAppearanceSubtype)
        matchedNextCourtAppearance.updateFrom(toUpdate)
      } ?: nextCourtAppearanceFunction(appearanceTypeCourtAppearanceSubtype).let { toCreateNextCourtAppearance ->
        val savedNextCourtAppearance = nextCourtAppearanceRepository.save(toCreateNextCourtAppearance)
        courtAppearanceRepository.updateNextCourtAppearance(
          savedNextCourtAppearance,
          performedByUser,
          ZonedDateTime.now(),
          matchedCourtAppearance,
        )
        courtAppearanceHistoryRepository.save(
          CourtAppearanceHistoryEntity.from(
            courtAppearanceRepository.findByIdOrNull(matchedCourtAppearance.id)!!,
            ChangeSource.NOMIS,
          ),
        )
      }
    }
  }

  private fun getMatchedNextCourtAppearanceOrLatest(courtCase: CourtCaseEntity, appearanceDateTime: LocalDateTime, courtAppearanceId: Int): CourtAppearanceEntity? = courtAppearanceRepository.findByNextEventDateTime(courtCase.id, appearanceDateTime) ?: courtAppearanceRepository.findFirstByCourtCaseAndStatusIdInAndIdNotOrderByAppearanceDateDesc(
    courtCase,
    listOf(CourtAppearanceEntityStatus.ACTIVE, CourtAppearanceEntityStatus.FUTURE),
    courtAppearanceId,
  )
}
