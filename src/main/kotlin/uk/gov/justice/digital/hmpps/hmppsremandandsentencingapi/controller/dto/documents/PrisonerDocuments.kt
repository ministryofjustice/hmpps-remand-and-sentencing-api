package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity

data class PrisonerDocuments(
  val courtCaseDocuments: List<CourtCaseDocuments>,
) {
  companion object {
    fun from(courtCases: Map<CourtCaseEntity, List<UploadedDocumentEntity>>): PrisonerDocuments = PrisonerDocuments(courtCases.map { CourtCaseDocuments.from(it) })
  }
}
