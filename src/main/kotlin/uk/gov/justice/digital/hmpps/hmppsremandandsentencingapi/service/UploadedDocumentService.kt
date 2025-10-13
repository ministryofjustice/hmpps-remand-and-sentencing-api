package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
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
  ) {
    val documentsToUnlink = uploadedDocumentRepository.findAllByAppearanceUUIDAndDocumentUuidNotIn(
      appearance.appearanceUuid,
      documentUUIDs,
    )
    unlinkDocuments(documentsToUnlink)

    documentUUIDs.forEach { documentId ->
      val document = uploadedDocumentRepository.findByDocumentUuid(documentId)
      if (document != null) {
        document.appearance = appearance
        document.updatedBy = serviceUserService.getUsername()
        document.updatedAt = ZonedDateTime.now()

        uploadedDocumentRepository.save(document)
      }
    }
  }

  @Transactional
  fun deleteDocumentsWithoutAppearanceId() {
    val cutoff = ZonedDateTime.now().minusDays(10)
    val documentsToBeDeleted = uploadedDocumentRepository.findDocumentUuidsWithoutAppearanceAndOlderThan10Days(cutoff)
    for (document in documentsToBeDeleted) {
      documentManagementApiClient.deleteDocument(documentId = document.documentUuid.toString())
    }
    uploadedDocumentRepository.deleteAll(documentsToBeDeleted)
  }

  private fun unlinkDocuments(uploadedDocuments: List<UploadedDocumentEntity>) {
    uploadedDocuments.forEach { document ->
      document.unlink(serviceUserService.getUsername())
      uploadedDocumentRepository.save(document)
    }
  }

  @Transactional(readOnly = true)
  fun getDocumentsByPrisonerId(prisonerId: String, searchDocuments: SearchDocuments): PrisonerDocuments {
    val prisonerDocuments = uploadedDocumentRepository.findByAppearanceCourtCasePrisonerId(prisonerId)

    val hasKeyword = !searchDocuments.keyword.isNullOrBlank()
    val hasTypes = searchDocuments.warrantTypeDocumentTypes.isNotEmpty()
    val hasCourtCodes = searchDocuments.courtCodes.isNotEmpty()
    val anyFilterActive = hasKeyword || hasTypes || hasCourtCodes

    val filtered = prisonerDocuments.filter { e ->
      val a = e.appearance!!

      val matchesKeyword =
        if (hasKeyword) {
          val kw = searchDocuments.keyword!!
          (a.courtCaseReference?.contains(kw, ignoreCase = true) == true) || e.fileName.contains(kw, ignoreCase = true)
        } else {
          false
        }

      val matchesWarrantTypeDocumentType =
        if (hasTypes) {
          searchDocuments.warrantTypeDocumentTypes.contains("${a.warrantType}|${e.documentType}")
        } else {
          false
        }

      val matchesCourtCode =
        if (hasCourtCodes) {
          searchDocuments.courtCodes.contains(a.courtCode)
        } else {
          false
        }

      if (!anyFilterActive) {
        true
      } else {
        (matchesKeyword || matchesWarrantTypeDocumentType || matchesCourtCode)
      }
    }
      .groupBy { it.appearance!!.courtCase }

    return PrisonerDocuments.from(filtered)
  }
}
