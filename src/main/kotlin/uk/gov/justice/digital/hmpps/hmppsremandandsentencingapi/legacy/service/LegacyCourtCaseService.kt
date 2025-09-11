package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.DraftAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyLinkCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.TestCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation.ReconciliationCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.domain.UnlinkEventsToEmit
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class LegacyCourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val serviceUserService: ServiceUserService, private val draftAppearanceRepository: DraftAppearanceRepository, private val chargeHistoryRepository: ChargeHistoryRepository, private val courtCaseHistoryRepository: CourtCaseHistoryRepository) {

  @Transactional
  fun create(courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
    val createdCourtCase = courtCaseRepository.save(
      CourtCaseEntity.from(
        courtCase,
        serviceUserService.getUsername(),
      ),
    )
    courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(createdCourtCase))
    return LegacyCourtCaseCreatedResponse(createdCourtCase.caseUniqueIdentifier)
  }

  @Transactional(readOnly = true)
  fun get(courtCaseUuid: String): LegacyCourtCase {
    val courtCase = getUnlessDeleted(courtCaseUuid)
    return LegacyCourtCase.from(courtCase)
  }

  @Transactional(readOnly = true)
  fun getTest(courtCaseUuid: String): TestCourtCase {
    val courtCase = getUnlessDeleted(courtCaseUuid)
    return TestCourtCase.from(courtCase)
  }

  @Transactional(readOnly = true)
  fun getReconciliation(courtCaseUuid: String): ReconciliationCourtCase {
    val courtCase = getUnlessDeleted(courtCaseUuid)
    return ReconciliationCourtCase.from(courtCase)
  }

  @Transactional
  fun update(courtCaseUuid: String, courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    existingCourtCase.statusId = if (courtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE
    existingCourtCase.legacyData = existingCourtCase.legacyData?.copyFrom(courtCase.legacyData) ?: courtCase.legacyData
    existingCourtCase.updatedAt = ZonedDateTime.now()
    existingCourtCase.updatedBy = serviceUserService.getUsername()
    courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(existingCourtCase))
    return LegacyCourtCaseCreatedResponse(existingCourtCase.caseUniqueIdentifier)
  }

  @Transactional
  fun linkCourtCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String, linkCase: LegacyLinkCase?): Pair<String, String> {
    val sourceCourtCase = getUnlessDeleted(sourceCourtCaseUuid)
    val targetCourtCase = getUnlessDeleted(targetCourtCaseUuid)
    sourceCourtCase.statusId = EntityStatus.MERGED
    sourceCourtCase.mergedToCase = targetCourtCase
    sourceCourtCase.mergedToDate = linkCase?.linkedDate ?: LocalDate.now()
    sourceCourtCase.updatedAt = ZonedDateTime.now()
    sourceCourtCase.updatedBy = serviceUserService.getUsername()
    courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(sourceCourtCase))
    return sourceCourtCaseUuid to sourceCourtCase.prisonerId
  }

  @Transactional
  fun unlinkCourtCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String): UnlinkEventsToEmit {
    val sourceCourtCase = getUnlessDeleted(sourceCourtCaseUuid)
    var courtCaseEventMetadata: EventMetadata? = null
    var chargeEventsToEmit = emptyList<EventMetadata>()
    if (sourceCourtCase.mergedToCase?.caseUniqueIdentifier == targetCourtCaseUuid) {
      sourceCourtCase.statusId = EntityStatus.ACTIVE
      sourceCourtCase.mergedToCase = null
      sourceCourtCase.mergedToDate = null
      sourceCourtCase.updatedAt = ZonedDateTime.now()
      sourceCourtCase.updatedBy = serviceUserService.getUsername()
      courtCaseEventMetadata = EventMetadataCreator.courtCaseEventMetadata(
        sourceCourtCase.prisonerId,
        sourceCourtCase.caseUniqueIdentifier,
        EventType.COURT_CASE_UPDATED,
      )
      chargeEventsToEmit = sourceCourtCase.appearances.filter { it.appearanceCharges.any { appearanceCharge -> appearanceCharge.charge!!.statusId == EntityStatus.MERGED } }
        .flatMap { appearance ->
          appearance.appearanceCharges.filter { appearanceCharge -> appearanceCharge.charge!!.statusId == EntityStatus.MERGED }
            .map { appearanceCharge ->
              val charge = appearanceCharge.charge!!
              charge.statusId = EntityStatus.ACTIVE
              charge.updatedAt = ZonedDateTime.now()
              charge.updatedBy = serviceUserService.getUsername()
              chargeHistoryRepository.save(ChargeHistoryEntity.from(charge))
              EventMetadataCreator.chargeEventMetadata(
                sourceCourtCase.prisonerId,
                sourceCourtCase.caseUniqueIdentifier,
                appearance.appearanceUuid.toString(),
                charge.chargeUuid.toString(),
                EventType.CHARGE_UPDATED,
              )
            }
        }
    }
    courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(sourceCourtCase))
    return UnlinkEventsToEmit(courtCaseEventMetadata, chargeEventsToEmit)
  }

  @Transactional
  fun delete(courtCaseUuid: String) {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    existingCourtCase.delete(serviceUserService.getUsername())
    courtCaseHistoryRepository.save(CourtCaseHistoryEntity.from(existingCourtCase))
    draftAppearanceRepository.deleteAll(existingCourtCase.draftAppearances)
  }

  private fun getUnlessDeleted(courtCaseUuid: String): CourtCaseEntity = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")
}
