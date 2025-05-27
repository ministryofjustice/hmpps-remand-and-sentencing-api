package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository

@Service
class UploadedDocumentService(
  private val uploadedDocumentRepository: UploadedDocumentRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
) {
  @Transactional
  fun create(createUploadedDocument: CreateUploadedDocument) {
    val courtAppearance = createUploadedDocument.appearanceUUID?.let {
      courtAppearanceRepository.findByAppearanceUuid(it)
        ?: throw EntityNotFoundException("No court appearance found with UUID $it")
    }

    createUploadedDocument.documents.map {
      UploadedDocumentEntity(
        documentUuid = it.documentUUID,
        documentType = it.documentType,
        appearance = courtAppearance,
        createdBy = createUploadedDocument.createdBy,
      )
    }.forEach(uploadedDocumentRepository::save)
  }
}
