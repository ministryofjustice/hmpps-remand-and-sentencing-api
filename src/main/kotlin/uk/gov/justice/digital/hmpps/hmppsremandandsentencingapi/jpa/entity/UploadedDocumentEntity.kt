package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "uploaded_document")
class UploadedDocumentEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val documentUuid: UUID = UUID.randomUUID(),
  @ManyToOne
  @JoinColumn(name = "appearance_id", referencedColumnName = "id", nullable = true)
  var appearance: CourtAppearanceEntity?,
  val documentType: String,
  val createdBy: String,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var fileName: String,
) {
  companion object {
    fun from(
      document: UploadedDocument,
      username: String,
      courtAppearanceEntity: CourtAppearanceEntity?,
    ): UploadedDocumentEntity = UploadedDocumentEntity(
      documentUuid = document.documentUUID,
      appearance = courtAppearanceEntity,
      documentType = document.documentType,
      createdBy = username,
      fileName = document.fileName,
    )
  }
}
