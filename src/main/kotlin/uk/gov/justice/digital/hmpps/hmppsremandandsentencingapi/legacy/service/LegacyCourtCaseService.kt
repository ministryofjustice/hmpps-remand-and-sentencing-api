package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseUuids
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyLinkCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUnlinkCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation.ReconciliationCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class LegacyCourtCaseService(
  private val courtCaseRepository: CourtCaseRepository,
  private val serviceUserService: ServiceUserService,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val courtCaseHistoryRepository: CourtCaseHistoryRepository,
  private val legacyAppearanceTypeService: LegacyAppearanceTypeService,
  private val legacyCourtAppearanceService: LegacyCourtAppearanceService,
) {

  @Transactional
  fun create(courtCase: LegacyCreateCourtCase): RecordResponse<LegacyCourtCaseCreatedResponse> {
    val createdCourtCase = courtCaseRepository.save(
      CourtCaseEntity.from(
        courtCase,
        getPerformedByUsername(courtCase),
      ),
    )
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        createdCourtCase,
        ChangeSource.NOMIS,
      ),
    )
    return RecordResponse(
      LegacyCourtCaseCreatedResponse(createdCourtCase.caseUniqueIdentifier),
      mutableSetOf(
        EventMetadataCreator.courtCaseEventMetadata(
          createdCourtCase.prisonerId,
          createdCourtCase.caseUniqueIdentifier,
          EventType.COURT_CASE_INSERTED,
        ),
      ),
    )
  }

  @Transactional(readOnly = true)
  fun get(courtCaseUuid: String): LegacyCourtCase {
    val courtCase = getUnlessDeleted(courtCaseUuid)
    return LegacyCourtCase.from(courtCase)
  }

  @WithSpan
  @Transactional(readOnly = true)
  fun getReconciliation(courtCaseUuid: String): ReconciliationCourtCase {
    val courtCase = getUnlessDeleted(courtCaseUuid)
    val defaultAppearanceType = legacyAppearanceTypeService.getDefaultAppearanceType()
    return ReconciliationCourtCase.from(courtCase, defaultAppearanceType)
  }

  @Transactional
  fun update(courtCaseUuid: String, courtCase: LegacyCreateCourtCase): RecordResponse<LegacyCourtCaseCreatedResponse> {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    val status = if (courtCase.active) CourtCaseEntityStatus.ACTIVE else CourtCaseEntityStatus.INACTIVE
    courtCaseRepository.updateLegacyDataBookingIdById(courtCase.bookingId ?: courtCase.legacyData.bookingId, status, ZonedDateTime.now(), getPerformedByUsername(courtCase), existingCourtCase.id)
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        courtCaseRepository.findByIdOrNull(existingCourtCase.id)!!,
        ChangeSource.NOMIS,
      ),
    )
    return RecordResponse(
      LegacyCourtCaseCreatedResponse(existingCourtCase.caseUniqueIdentifier),
      mutableSetOf(
        EventMetadataCreator.courtCaseEventMetadata(
          courtCase.prisonerId,
          existingCourtCase.caseUniqueIdentifier,
          EventType.COURT_CASE_UPDATED,
        ),
      ),
    )
  }

  private fun getPerformedByUsername(courtCase: LegacyCreateCourtCase): String = courtCase.performedByUser ?: serviceUserService.getUsername()

  @Transactional
  fun linkCourtCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String, linkCase: LegacyLinkCase?): MutableSet<EventMetadata> {
    val sourceCourtCase = getUnlessDeleted(sourceCourtCaseUuid)
    val targetCourtCase = getUnlessDeleted(targetCourtCaseUuid)
    sourceCourtCase.statusId = CourtCaseEntityStatus.MERGED
    sourceCourtCase.mergedToCase = targetCourtCase
    sourceCourtCase.mergedToDate = linkCase?.linkedDate ?: LocalDate.now()
    sourceCourtCase.updatedAt = ZonedDateTime.now()
    sourceCourtCase.updatedBy = linkCase?.performedByUser ?: serviceUserService.getUsername()
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        sourceCourtCase,
        ChangeSource.NOMIS,
      ),
    )
    return mutableSetOf(
      EventMetadataCreator.courtCaseEventMetadata(
        sourceCourtCase.prisonerId,
        sourceCourtCaseUuid,
        EventType.COURT_CASE_UPDATED,
      ),
    )
  }

  @Transactional
  fun unlinkCourtCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String, unlinkCase: LegacyUnlinkCase?): MutableSet<EventMetadata> {
    val sourceCourtCase = getUnlessDeleted(sourceCourtCaseUuid)
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val performedByUsername = unlinkCase?.performedByUser ?: serviceUserService.getUsername()
    if (sourceCourtCase.statusId != CourtCaseEntityStatus.ACTIVE || sourceCourtCase.mergedToCase != null) {
      sourceCourtCase.statusId = CourtCaseEntityStatus.ACTIVE
      sourceCourtCase.mergedToCase = null
      sourceCourtCase.mergedToDate = null
      sourceCourtCase.updatedAt = ZonedDateTime.now()
      sourceCourtCase.updatedBy = performedByUsername
      eventsToEmit.add(
        EventMetadataCreator.courtCaseEventMetadata(
          sourceCourtCase.prisonerId,
          sourceCourtCase.caseUniqueIdentifier,
          EventType.COURT_CASE_UPDATED,
        ),
      )
      courtCaseHistoryRepository.save(
        CourtCaseHistoryEntity.from(
          sourceCourtCase,
          ChangeSource.NOMIS,
        ),
      )
    }
    eventsToEmit.addAll(
      sourceCourtCase.appearances.filter { it.appearanceCharges.any { appearanceCharge -> appearanceCharge.charge!!.statusId == ChargeEntityStatus.MERGED } }
        .flatMap { appearance ->
          appearance.appearanceCharges.filter { appearanceCharge -> appearanceCharge.charge!!.statusId == ChargeEntityStatus.MERGED }
            .map { appearanceCharge ->
              val charge = appearanceCharge.charge!!
              charge.statusId = ChargeEntityStatus.ACTIVE
              charge.updatedAt = ZonedDateTime.now()
              charge.updatedBy = performedByUsername
              chargeHistoryRepository.save(
                ChargeHistoryEntity.from(
                  charge,
                  ChangeSource.NOMIS,
                ),
              )
              EventMetadataCreator.chargeEventMetadata(
                sourceCourtCase.prisonerId,
                sourceCourtCase.caseUniqueIdentifier,
                appearance.appearanceUuid.toString(),
                charge.chargeUuid.toString(),
                EventType.CHARGE_UPDATED,
                appearance.statusId == CourtAppearanceEntityStatus.FUTURE,
                false,
              )
            }
        },
    )

    return eventsToEmit
  }

  @Retryable(maxAttempts = 3, retryFor = [OptimisticLockingFailureException::class])
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun delete(courtCaseUuid: String, performedByUser: String?): MutableSet<EventMetadata> {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val performedByUsername = performedByUser ?: serviceUserService.getUsername()
    existingCourtCase.delete(performedByUsername)
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        existingCourtCase,
        ChangeSource.NOMIS,
      ),
    )
    eventsToEmit.add(
      EventMetadataCreator.courtCaseEventMetadata(
        existingCourtCase.prisonerId,
        courtCaseUuid,
        EventType.COURT_CASE_DELETED,
      ),
    )
    val appearanceEvents = existingCourtCase.appearances.filter { it.statusId != CourtAppearanceEntityStatus.DELETED }.flatMap { legacyCourtAppearanceService.deleteCourtAppearance(it, performedByUsername) }
    eventsToEmit.addAll(appearanceEvents)
    return eventsToEmit
  }

  private fun getUnlessDeleted(courtCaseUuid: String): CourtCaseEntity = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)
    ?.takeUnless { entity -> entity.statusId == CourtCaseEntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")

  @Transactional(readOnly = true)
  fun getCourtCaseUuids(prisonerId: String): LegacyCourtCaseUuids = LegacyCourtCaseUuids(courtCaseRepository.findCaseUniqueIdentifierByPrisonerIdAndStatusIdNot(prisonerId))
}
