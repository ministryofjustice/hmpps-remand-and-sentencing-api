package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.PrisonerDocuments
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.SearchDocuments
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
  ) {
    val documentsToUnlink =
      uploadedDocumentRepository.findAllByAppearanceUUIDAndDocumentUuidNotIn(
        appearance.appearanceUuid,
        documentUUIDs,
      )

    unlinkDocuments(documentsToUnlink, prisonerId)

    val linkedDocumentIds = mutableListOf<UUID>()

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

    linkedDocumentIds.forEach { documentId ->
      safelyExecuteApiCall(
        actionName = "updateMetadataActive",
        documentId = documentId,
        prisonerId = prisonerId,
      ) {
        documentManagementApiClient.updateDocumentMetadata(
          prisonerId = prisonerId,
          documentId = documentId.toString(),
          uploadStatus = "Active",
        )
      }
    }
  }

  fun unlinkDocuments(
    uploadedDocuments: List<UploadedDocumentEntity>,
    prisonerId: String,
  ) {
    // ✅ Step 1: DB updates FIRST
    uploadedDocuments.forEach { document ->
      document.unlink(serviceUserService.getUsername())
      uploadedDocumentRepository.save(document)
    }

    // ✅ Step 2: External calls (safe)
    uploadedDocuments.forEach { document ->
      safelyExecuteApiCall(
        actionName = "updateMetadataDeleted",
        documentId = document.documentUuid,
        prisonerId = prisonerId,
      ) {
        documentManagementApiClient.updateDocumentMetadata(
          prisonerId = prisonerId,
          documentId = document.documentUuid.toString(),
          uploadStatus = "Deleted",
        )
      }
    }
  }

  @Transactional
  fun deleteDocumentsWithoutAppearanceId() {
    val cutoff = ZonedDateTime.now().minusDays(10)
    val documentsToBeDeleted =
      uploadedDocumentRepository.findDocumentUuidsWithoutAppearanceAndOlderThan10Days(cutoff)

    documentsToBeDeleted.forEach { document ->
      safelyExecuteApiCall(
        actionName = "deleteDocument",
        documentId = document.documentUuid,
        prisonerId = "system",
      ) {
        documentManagementApiClient.deleteDocument(
          documentId = document.documentUuid.toString(),
        )
      }
    }

    uploadedDocumentRepository.deleteAll(documentsToBeDeleted)
  }

  @Transactional(readOnly = true)
  fun getDocumentsByPrisonerId(prisonerId: String, searchDocuments: SearchDocuments): PrisonerDocuments {
    val prisonerDocuments = uploadedDocumentRepository.findByAppearanceCourtCasePrisonerId(prisonerId)

    val filtered = prisonerDocuments.filter { uploadedDocument ->
      val appearance = uploadedDocument.appearance!!
      val matchesKeyword = searchDocuments.keyword?.let {
        appearance.courtCaseReference?.contains(it, true) == true ||
          uploadedDocument.fileName.contains(it, true)
      } == true
      val matchesType = searchDocuments.warrantTypeDocumentTypes.contains("${appearance.warrantType}|${uploadedDocument.documentType}")
      val matchesCourt = searchDocuments.courtCodes.contains(appearance.courtCode)

      searchDocuments.isEmpty() || matchesKeyword || matchesType || matchesCourt
    }.groupBy { it.appearance!!.courtCase }

    return PrisonerDocuments.from(filtered)
  }

  private fun safelyExecuteApiCall(
    actionName: String,
    documentId: UUID?,
    prisonerId: String,
    action: () -> Unit,
  ) {
    try {
      action()
    } catch (e: Exception) {
      log.error(
        "External call failed: action={}, documentId={}, prisonerId={}, message={}",
        actionName,
        documentId,
        prisonerId,
        e.message,
        e,
      )
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(UploadedDocumentService::class.java)
  }
}
