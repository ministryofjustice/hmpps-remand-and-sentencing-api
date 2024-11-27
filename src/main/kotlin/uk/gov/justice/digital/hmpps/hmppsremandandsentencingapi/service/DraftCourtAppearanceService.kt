package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.DraftAppearanceRepository
import java.util.UUID

@Service
class DraftCourtAppearanceService(private val draftAppearanceRepository: DraftAppearanceRepository, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun update(draftUuid: UUID, draftAppearance: DraftCreateCourtAppearance) {
    val existingDraftAppearance = getOrThrow(draftUuid)
    draftAppearanceRepository.save(existingDraftAppearance.copyFrom(draftAppearance, serviceUserService.getUsername()))
  }

  fun get(draftUuid: UUID): DraftCourtAppearance {
    val existingDraftAppearance = getOrThrow(draftUuid)
    return DraftCourtAppearance.from(existingDraftAppearance)
  }

  fun getOrThrow(draftUuid: UUID): DraftAppearanceEntity {
    return draftAppearanceRepository.findByDraftUuid(draftUuid) ?: throw EntityNotFoundException("No draft appearance found at $draftUuid")
  }
}
