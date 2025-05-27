package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "uploaded_document")
class UploadedDocumentEntity(
  @Id
  val documentUuid: UUID = UUID.randomUUID(),
  @ManyToOne
  @JoinColumn(name = "appearance_id", referencedColumnName = "id", nullable = false)
  var appearance: CourtAppearanceEntity?,
  val documentType: String,
  val warrantType: String,
  val createdBy: String,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
)
