package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.util.*

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
        warrantType = it.warrantType,
        appearance = courtAppearance,
      )
    }.forEach(uploadedDocumentRepository::save)
  }

  @Transactional
  fun update(documentUuids: List<UUID>, appearanceUuid: UUID) {
    val courtAppearance = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)
      ?: throw EntityNotFoundException("No court appearance found with UUID $appearanceUuid")

    documentUuids.map { documentUuid ->
      uploadedDocumentRepository.findByDocumentUuid(documentUuid)
        .orElseThrow { EntityNotFoundException("No uploaded document found with UUID $documentUuid") }
        .apply { appearance = courtAppearance }
    }.forEach(uploadedDocumentRepository::save)
  }

  fun findAllByAppearanceUUIDAndWarrantType(appearanceUuid: UUID, warrantType: String): List<UploadedDocument> {
    return uploadedDocumentRepository.findAllByAppearanceUUIDAndWarrantType(
      appearanceUuid,
      warrantType,
    ).map { uploadedDocumentEntity ->
      UploadedDocument.from(uploadedDocumentEntity)
    }
  }


}