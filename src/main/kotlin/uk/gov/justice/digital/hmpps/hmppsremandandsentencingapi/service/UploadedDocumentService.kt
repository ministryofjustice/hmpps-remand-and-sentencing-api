package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.util.UUID

@Service
class UploadedDocumentService(
  private val uploadedDocumentRepository: UploadedDocumentRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val serviceUserService: ServiceUserService,
) {
  @Transactional
  fun create(createUploadedDocument: CreateUploadedDocument) {
    val courtAppearance = createUploadedDocument.appearanceUUID?.let {
      courtAppearanceRepository.findByAppearanceUuid(it)
        ?: throw EntityNotFoundException("No court appearance found with UUID $it")
    }

    createUploadedDocument.documents.map {
      UploadedDocumentEntity.from(it, serviceUserService.getUsername(), courtAppearance)
    }.forEach(uploadedDocumentRepository::save)
  }

  @Transactional
  fun update(
    documentUUIDs: List<UUID>,
    appearanceUUID: UUID,
  ) {
    documentUUIDs.forEach { documentId ->
      val document = uploadedDocumentRepository.findByDocumentUuid(documentId)
      if (document != null) {
        val newAppearance = courtAppearanceRepository.findByAppearanceUuid(appearanceUUID)
          ?: throw EntityNotFoundException("No court appearance found with UUID $appearanceUUID")

        document.appearance = newAppearance
        document.updatedBy = serviceUserService.getUsername()

        uploadedDocumentRepository.save(document)
      }
    }
  }
}
