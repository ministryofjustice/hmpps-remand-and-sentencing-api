package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import java.time.LocalDate
import java.util.UUID

data class AppearanceDocument(
  val documentUUID: UUID,
  val documentType: String,
  val fileName: String,
  val warrantDate: LocalDate,
  val caseReference: String?,
  val courtCode: String,
  val warrantType: String,
) {
  companion object {
    fun from(uploadedDocumentEntity: UploadedDocumentEntity): AppearanceDocument = AppearanceDocument(
      uploadedDocumentEntity.documentUuid,
      uploadedDocumentEntity.documentType,
      uploadedDocumentEntity.fileName,
      uploadedDocumentEntity.appearance!!.appearanceDate,
      uploadedDocumentEntity.appearance!!.courtCaseReference,
      uploadedDocumentEntity.appearance!!.courtCode,
      uploadedDocumentEntity.appearance!!.warrantType,
    )
  }
}
