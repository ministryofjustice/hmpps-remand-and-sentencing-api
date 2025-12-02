package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.domain.UnlinkEventsToEmit
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class LegacyCourtCaseService(
  private val courtCaseRepository: CourtCaseRepository,
  private val serviceUserService: ServiceUserService,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val courtCaseHistoryRepository: CourtCaseHistoryRepository,
) {

  @Transactional
  fun create(courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
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
    return LegacyCourtCaseCreatedResponse(createdCourtCase.caseUniqueIdentifier)
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
    return ReconciliationCourtCase.from(courtCase)
  }

  @Transactional
  fun update(courtCaseUuid: String, courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    val status = if (courtCase.active) CourtCaseEntityStatus.ACTIVE else CourtCaseEntityStatus.INACTIVE
    courtCaseRepository.updateLegacyDataBookingIdById(courtCase.bookingId, status, ZonedDateTime.now(), getPerformedByUsername(courtCase), existingCourtCase.id)
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        courtCaseRepository.findByIdOrNull(existingCourtCase.id)!!,
        ChangeSource.NOMIS,
      ),
    )
    return LegacyCourtCaseCreatedResponse(existingCourtCase.caseUniqueIdentifier)
  }

  private fun getPerformedByUsername(courtCase: LegacyCreateCourtCase): String = courtCase.performedByUser ?: serviceUserService.getUsername()

  @Transactional
  fun linkCourtCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String, linkCase: LegacyLinkCase?): Pair<String, String> {
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
    return sourceCourtCaseUuid to sourceCourtCase.prisonerId
  }

  @Transactional
  fun unlinkCourtCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String, unlinkCase: LegacyUnlinkCase?): UnlinkEventsToEmit {
    val sourceCourtCase = getUnlessDeleted(sourceCourtCaseUuid)
    var courtCaseEventMetadata: EventMetadata? = null
    var chargeEventsToEmit = emptyList<EventMetadata>()
    val performedByUsername = unlinkCase?.performedByUser ?: serviceUserService.getUsername()
    if (sourceCourtCase.mergedToCase?.caseUniqueIdentifier == targetCourtCaseUuid) {
      sourceCourtCase.statusId = CourtCaseEntityStatus.ACTIVE
      sourceCourtCase.mergedToCase = null
      sourceCourtCase.mergedToDate = null
      sourceCourtCase.updatedAt = ZonedDateTime.now()
      sourceCourtCase.updatedBy = performedByUsername
      courtCaseEventMetadata = EventMetadataCreator.courtCaseEventMetadata(
        sourceCourtCase.prisonerId,
        sourceCourtCase.caseUniqueIdentifier,
        EventType.COURT_CASE_UPDATED,
      )
      chargeEventsToEmit = sourceCourtCase.appearances.filter { it.appearanceCharges.any { appearanceCharge -> appearanceCharge.charge!!.statusId == ChargeEntityStatus.MERGED } }
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
              )
            }
        }
    }
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        sourceCourtCase,
        ChangeSource.NOMIS,
      ),
    )
    return UnlinkEventsToEmit(courtCaseEventMetadata, chargeEventsToEmit)
  }

  @Transactional
  fun delete(courtCaseUuid: String, performedByUser: String?) {
    val existingCourtCase = getUnlessDeleted(courtCaseUuid)
    existingCourtCase.delete(performedByUser ?: serviceUserService.getUsername())
    courtCaseHistoryRepository.save(
      CourtCaseHistoryEntity.from(
        existingCourtCase,
        ChangeSource.NOMIS,
      ),
    )
  }

  private fun getUnlessDeleted(courtCaseUuid: String): CourtCaseEntity = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)
    ?.takeUnless { entity -> entity.statusId == CourtCaseEntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")

  @Transactional(readOnly = true)
  fun getCourtCaseUuids(prisonerId: String): LegacyCourtCaseUuids = LegacyCourtCaseUuids(courtCaseRepository.findCaseUniqueIdentifierByPrisonerIdAndStatusIdNot(prisonerId))
}
