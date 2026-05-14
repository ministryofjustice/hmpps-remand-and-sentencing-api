package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.PrisonerDocuments
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.SearchDocuments
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.DocumentMetadataStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.DocumentMetadataUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UploadedDocumentService(
  private val uploadedDocumentRepository: UploadedDocumentRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val serviceUserService: ServiceUserService,
  private val documentManagementApiClient: DocumentManagementApiClient,
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
    appearance: CourtAppearanceEntity,
    prisonerId: String,
  ): MutableList<DocumentMetadataUpdate> {
    val documentMetadataUpdates = mutableListOf<DocumentMetadataUpdate>()
    val linkedDocumentIds = mutableListOf<UUID>()
    val documentsToUnlink =
      uploadedDocumentRepository.findAllByAppearanceUUIDAndDocumentUuidNotIn(
        appearance.appearanceUuid,
        documentUUIDs,
      )
    unlinkDocuments(documentsToUnlink)
    documentsToUnlink.forEach { document -> documentMetadataUpdates.add(DocumentMetadataUpdate(prisonerId, document.documentUuid, DocumentMetadataStatus.DELETED)) }

    documentUUIDs.forEach { documentId ->
      val document = uploadedDocumentRepository.findByDocumentUuid(documentId)
      if (document != null) {
        document.appearance = appearance
        document.updatedBy = serviceUserService.getUsername()
        document.updatedAt = ZonedDateTime.now()

        uploadedDocumentRepository.save(document)
        linkedDocumentIds.add(documentId)
      }
    }

    linkedDocumentIds.forEach {
      documentMetadataUpdates.add(DocumentMetadataUpdate(prisonerId, it, DocumentMetadataStatus.ACTIVE))
    }

    return documentMetadataUpdates
  }

  @Transactional
  fun deleteDocumentsWithoutAppearanceId() {
    val cutoff = ZonedDateTime.now().minusDays(10)
    val documentsToBeDeleted = uploadedDocumentRepository.findDocumentUuidsWithoutAppearanceAndOlderThan10Days(cutoff)
    for (document in documentsToBeDeleted) {
      documentManagementApiClient.deleteDocument(documentId = document.documentUuid.toString())
    }
  }

  fun unlinkDocuments(
    uploadedDocuments: List<UploadedDocumentEntity>,
  ) {
    uploadedDocuments.forEach { document ->
      document.unlink(serviceUserService.getUsername())
      uploadedDocumentRepository.save(document)
    }
  }

  @Transactional(readOnly = true)
  fun getDocumentsByPrisonerId(prisonerId: String, searchDocuments: SearchDocuments): PrisonerDocuments {
    val prisonerDocuments = uploadedDocumentRepository.findByAppearanceCourtCasePrisonerId(prisonerId)

    val filtered = prisonerDocuments.filter { uploadedDocumentEntity ->
      val appearance = uploadedDocumentEntity.appearance!!
      val matchesKeyword = searchDocuments.keyword?.let { appearance.courtCaseReference?.contains(it, true) == true || uploadedDocumentEntity.fileName.contains(it, true) } == true
      val matchesWarrantTypeDocumentType = searchDocuments.warrantTypeDocumentTypes.contains("${appearance.warrantType}|${uploadedDocumentEntity.documentType}")
      val matchesCourtCode = searchDocuments.courtCodes.contains(appearance.courtCode)
      searchDocuments.isEmpty() || matchesKeyword || matchesWarrantTypeDocumentType || matchesCourtCode
    }
      .groupBy { it.appearance!!.courtCase }

    return PrisonerDocuments.from(filtered)
  }

  fun processDocumentMetadataUpdates(
    updates: List<DocumentMetadataUpdate>,
  ) {
    updates.forEach { update ->
      try {
        documentManagementApiClient.updateDocumentMetadata(
          prisonerId = update.prisonerId,
          documentId = update.documentId.toString(),
          uploadStatus = update.status,
        )
      } catch (e: Exception) {
        log.warn(
          "Failed to update metadata for document {} with status {}",
          update.documentId,
          update.status,
          e,
        )
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(UploadedDocumentService::class.java)
  }
}
