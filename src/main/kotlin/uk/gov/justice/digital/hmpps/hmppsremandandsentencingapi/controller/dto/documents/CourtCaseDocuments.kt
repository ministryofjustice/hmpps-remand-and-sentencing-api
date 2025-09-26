package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity

data class CourtCaseDocuments(
  val courtCaseUuid: String,
  val latestAppearance: CourtAppearance?,
  val appearanceDocumentsByType: Map<String, List<AppearanceDocument>>,
) {
  companion object {
    fun from(courtCaseEntry: Map.Entry<CourtCaseEntity, List<UploadedDocumentEntity>>): CourtCaseDocuments = CourtCaseDocuments(
      courtCaseEntry.key.caseUniqueIdentifier,
      CourtAppearance.from(courtCaseEntry.key.latestCourtAppearance!!),
      courtCaseEntry.value.groupBy { it.documentType }.mapValues { (_, documents) -> documents.map { AppearanceDocument.from(it) } },
    )
  }
}
