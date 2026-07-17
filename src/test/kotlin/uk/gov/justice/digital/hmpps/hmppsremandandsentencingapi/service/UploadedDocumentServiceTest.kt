package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.util.UUID

class UploadedDocumentServiceTest {

  private val uploadedDocumentRepository = mockk<UploadedDocumentRepository>(relaxed = true)
  private val courtAppearanceRepository = mockk<CourtAppearanceRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val documentManagementApiClient = mockk<DocumentManagementApiClient>()

  private val uploadedDocumentService = UploadedDocumentService(
    uploadedDocumentRepository = uploadedDocumentRepository,
    courtAppearanceRepository = courtAppearanceRepository,
    serviceUserService = serviceUserService,
    documentManagementApiClient = documentManagementApiClient,
  )

  init {
    every { serviceUserService.getUsername() } returns "test-user"
    every { uploadedDocumentRepository.save(any<UploadedDocumentEntity>()) } answers { firstArg() }
  }

  @Test
  fun `should create a new document entity when no existing row matches the document uuid`() {
    val documentUuid = UUID.randomUUID()
    val document = UploadedDocument(documentUuid, "HMCTS_WARRANT", "warrant.pdf")
    every { uploadedDocumentRepository.findByDocumentUuid(documentUuid) } returns null

    uploadedDocumentService.create(CreateUploadedDocument(appearanceUUID = null, documents = listOf(document)))

    val savedSlot = slot<UploadedDocumentEntity>()
    verify(exactly = 1) { uploadedDocumentRepository.save(capture(savedSlot)) }
    assertThat(savedSlot.captured.documentUuid).isEqualTo(documentUuid)
    assertThat(savedSlot.captured.fileName).isEqualTo("warrant.pdf")
    assertThat(savedSlot.captured.appearance).isNull()
  }

  @Test
  fun `should update the existing row instead of inserting a duplicate when the document uuid already exists`() {
    val documentUuid = UUID.randomUUID()
    val existing = UploadedDocumentEntity(
      id = 42,
      documentUuid = documentUuid,
      appearance = null,
      documentType = "HMCTS_WARRANT",
      createdBy = "original-user",
      fileName = "old-name.pdf",
    )
    val document = UploadedDocument(documentUuid, "HMCTS_WARRANT", "new-name.pdf")
    every { uploadedDocumentRepository.findByDocumentUuid(documentUuid) } returns existing

    uploadedDocumentService.create(CreateUploadedDocument(appearanceUUID = null, documents = listOf(document)))

    val savedSlot = slot<UploadedDocumentEntity>()
    verify(exactly = 1) { uploadedDocumentRepository.save(capture(savedSlot)) }
    assertThat(savedSlot.captured.id).isEqualTo(42)
    assertThat(savedSlot.captured.fileName).isEqualTo("new-name.pdf")
    assertThat(savedSlot.captured.updatedBy).isEqualTo("test-user")
    assertThat(savedSlot.captured.updatedAt).isNotNull()
  }

  @Test
  fun `should link the resolved court appearance onto both new and updated document rows`() {
    val appearanceUuid = UUID.randomUUID()
    val courtAppearance = mockk<CourtAppearanceEntity>()
    every { courtAppearanceRepository.findByAppearanceUuid(appearanceUuid) } returns courtAppearance

    val newDocumentUuid = UUID.randomUUID()
    val existingDocumentUuid = UUID.randomUUID()
    val existing = UploadedDocumentEntity(
      id = 7,
      documentUuid = existingDocumentUuid,
      appearance = null,
      documentType = "HMCTS_WARRANT",
      createdBy = "original-user",
      fileName = "existing.pdf",
    )
    every { uploadedDocumentRepository.findByDocumentUuid(newDocumentUuid) } returns null
    every { uploadedDocumentRepository.findByDocumentUuid(existingDocumentUuid) } returns existing

    uploadedDocumentService.create(
      CreateUploadedDocument(
        appearanceUUID = appearanceUuid,
        documents = listOf(
          UploadedDocument(newDocumentUuid, "HMCTS_WARRANT", "new.pdf"),
          UploadedDocument(existingDocumentUuid, "HMCTS_WARRANT", "existing-updated.pdf"),
        ),
      ),
    )

    verify(exactly = 2) { uploadedDocumentRepository.save(match { it.appearance == courtAppearance }) }
  }

  @Test
  fun `should throw when the appearance uuid does not resolve to a court appearance`() {
    val appearanceUuid = UUID.randomUUID()
    every { courtAppearanceRepository.findByAppearanceUuid(appearanceUuid) } returns null

    assertThatThrownBy {
      uploadedDocumentService.create(CreateUploadedDocument(appearanceUUID = appearanceUuid, documents = emptyList()))
    }.isInstanceOf(EntityNotFoundException::class.java)
  }
}
