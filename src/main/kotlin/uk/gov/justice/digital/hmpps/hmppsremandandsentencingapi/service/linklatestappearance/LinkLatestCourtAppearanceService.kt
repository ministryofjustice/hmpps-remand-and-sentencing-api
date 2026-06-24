package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.linklatestappearance

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyAppearanceTypeService
import java.time.ZonedDateTime

@Service
class LinkLatestCourtAppearanceService(private val courtAppearanceRepository: CourtAppearanceRepository, private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository, private val legacyAppearanceTypeService: LegacyAppearanceTypeService) {

  @Async
  @Transactional
  fun linkLatestCourtAppearances() {
    val latestCourtAppearanceDataRows = courtAppearanceRepository.getLatestCourtAppearancesWithoutNextCourtAppearance().groupBy { LatestCourtAppearanceKey(it.courtCaseId, it.latestAppearanceId) }
    latestCourtAppearanceDataRows.forEach { (key, rows) ->
      if (rows.all { it.nextCourtAppearanceId == null }) {
        createMissingNextCourtAppearance(key, rows)
      } else {
        moveExistingNextCourtAppearance(key, rows)
      }
    }
  }

  private fun createMissingNextCourtAppearance(key: LatestCourtAppearanceKey, rows: List<LatestCourtAppearanceDataRow>) {
    log.debug("Creating missing next court appearance for court case ${key.courtCaseId} and court appearance ${key.latestAppearanceId}")
    val futureAppearance = courtAppearanceRepository.findFirstByIdInOrderByCreatedAtAsc(rows.map { it.futureAppearanceId })
    val latestCourtAppearance = courtAppearanceRepository.findByIdOrNull(key.latestAppearanceId)!!
    val appearanceTypeCourtAppearanceSubtype = legacyAppearanceTypeService.getAppearanceType(futureAppearance.legacyData?.nomisAppearanceTypeCode, null)
    val nextCourtAppearance = nextCourtAppearanceRepository.save(NextCourtAppearanceEntity.from(futureAppearance, appearanceTypeCourtAppearanceSubtype))
    latestCourtAppearance.nextCourtAppearance = nextCourtAppearance
    latestCourtAppearance.updatedBy = UPDATED_BY
    latestCourtAppearance.updatedAt = ZonedDateTime.now()
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(latestCourtAppearance, ChangeSource.DPS))
  }

  private fun moveExistingNextCourtAppearance(key: LatestCourtAppearanceKey, rows: List<LatestCourtAppearanceDataRow>) {
    val nextCourtAppearanceIds = rows.filter { it.nextCourtAppearanceId != null }.map { it.nextCourtAppearanceId!! }.distinct()
    if (nextCourtAppearanceIds.size == 1) {
      log.debug("In court case ${key.courtCaseId} and court appearance ${key.latestAppearanceId} moving next court appearance ${nextCourtAppearanceIds.first()}")
      moveSingleNextCourtAppearance(key, rows, nextCourtAppearanceIds.first())
    }
    if (nextCourtAppearanceIds.size > 1) {
      log.debug("Court case ${key.courtCaseId} and court appearance ${key.latestAppearanceId} has more than 1 next court appearance ${nextCourtAppearanceIds.joinToString()} and the incorrect appearances are ${rows.filter { it.incorrectAppearanceId != null }.map { it.incorrectAppearanceId!! }.joinToString()}")
      moveMultipleNextCourtAppearances(key, rows)
    }
  }

  private fun moveSingleNextCourtAppearance(key: LatestCourtAppearanceKey, rows: List<LatestCourtAppearanceDataRow>, nextCourtAppearanceId: Int) {
    val latestCourtAppearance = courtAppearanceRepository.findByIdOrNull(key.latestAppearanceId)!!
    val incorrectCourtAppearances = courtAppearanceRepository.findAllById(rows.filter { it.incorrectAppearanceId != null }.map { it.incorrectAppearanceId!! }.distinct())
    val nextCourtAppearance = nextCourtAppearanceRepository.findByIdOrNull(nextCourtAppearanceId)!!
    latestCourtAppearance.nextCourtAppearance = nextCourtAppearance
    latestCourtAppearance.updatedBy = UPDATED_BY
    latestCourtAppearance.updatedAt = ZonedDateTime.now()
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(latestCourtAppearance, ChangeSource.DPS))
    incorrectCourtAppearances.forEach { incorrectCourtAppearance ->
      incorrectCourtAppearance.nextCourtAppearance = null
      incorrectCourtAppearance.updatedBy = UPDATED_BY
      incorrectCourtAppearance.updatedAt = ZonedDateTime.now()
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(incorrectCourtAppearance, ChangeSource.DPS))
    }
  }

  private fun moveMultipleNextCourtAppearances(key: LatestCourtAppearanceKey, rows: List<LatestCourtAppearanceDataRow>) {
    val latestCourtAppearance = courtAppearanceRepository.findByIdOrNull(key.latestAppearanceId)!!
    val incorrectCourtAppearances = courtAppearanceRepository.findAllById(rows.filter { it.incorrectAppearanceId != null }.map { it.incorrectAppearanceId!! }.distinct())
    val incorrectCourtAppearance = incorrectCourtAppearances.maxBy { it.createdAt }
    val nextCourtAppearance = incorrectCourtAppearance.nextCourtAppearance!!
    latestCourtAppearance.nextCourtAppearance = nextCourtAppearance
    latestCourtAppearance.updatedBy = UPDATED_BY
    latestCourtAppearance.updatedAt = ZonedDateTime.now()
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(latestCourtAppearance, ChangeSource.DPS))
    incorrectCourtAppearance.nextCourtAppearance = null
    incorrectCourtAppearance.updatedBy = UPDATED_BY
    incorrectCourtAppearance.updatedAt = ZonedDateTime.now()
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(incorrectCourtAppearance, ChangeSource.DPS))
    incorrectCourtAppearances.filter { it.id != incorrectCourtAppearance.id }.forEach {
      it.nextCourtAppearance = null
      it.updatedBy = UPDATED_BY
      it.updatedAt = ZonedDateTime.now()
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(it, ChangeSource.DPS))
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val UPDATED_BY: String = "SYSTEM"
  }
}
