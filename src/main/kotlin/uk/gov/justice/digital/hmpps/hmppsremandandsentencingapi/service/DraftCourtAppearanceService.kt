package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.DraftAppearanceRepository
import java.util.UUID

@Service
class DraftCourtAppearanceService(private val draftAppearanceRepository: DraftAppearanceRepository, private val serviceUserService: ServiceUserService) {

  @Transactional
  fun update(draftUuid: UUID, draftAppearance: DraftCreateCourtAppearance) {
    val existingDraftAppearance = draftAppearanceRepository.findByDraftUuid(draftUuid) ?: throw EntityNotFoundException("No draft appearance found at $draftUuid")
    draftAppearanceRepository.save(existingDraftAppearance.copyFrom(draftAppearance, serviceUserService.getUsername()))
  }
}
