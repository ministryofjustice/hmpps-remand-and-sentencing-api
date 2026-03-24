package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceSubtype
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceSubtypeRepository
import java.util.UUID

@Service
class CourtAppearanceSubtypeService(private val courtAppearanceSubtypeRepository: CourtAppearanceSubtypeRepository) {

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<CourtAppearanceSubtype> = courtAppearanceSubtypeRepository.findByStatusIn(statuses).map { CourtAppearanceSubtype.from(it) }

  fun findByUuid(courtAppearanceSubtypeUuid: UUID): CourtAppearanceSubtype? = courtAppearanceSubtypeRepository.findByAppearanceSubtypeUuid(courtAppearanceSubtypeUuid)?.let { CourtAppearanceSubtype.from(it) }
}
