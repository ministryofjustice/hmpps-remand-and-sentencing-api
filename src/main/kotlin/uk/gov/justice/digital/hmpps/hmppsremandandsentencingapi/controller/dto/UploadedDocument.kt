package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import java.util.UUID

data class UploadedDocument(
  val documentUUID: UUID,
  val documentType: String,
  val fileName: String,
  /* This field will need to be inserted in the entity class once the decision is made for showing court data ingested
  document in the RAS Edit journeys */
  val courtDataIngested: Boolean = false,
) {
  companion object {
    fun from(uploadedDocumentEntity: UploadedDocumentEntity): UploadedDocument = UploadedDocument(
      documentUUID = uploadedDocumentEntity.documentUuid,
      documentType = uploadedDocumentEntity.documentType,
      fileName = uploadedDocumentEntity.fileName,
    )
  }
}
