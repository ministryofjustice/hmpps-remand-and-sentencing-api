import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.UploadedDocumentService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.*

class UploadedDocumentServiceTest {
  private val uploadedDocumentRepository = mockk<UploadedDocumentRepository>(relaxed = true)
  private val courtAppearanceRepository = mockk<CourtAppearanceRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val documentManagementApiClient = mockk<DocumentManagementApiClient>()
  private var idInt: Int = 0

  private val service = UploadedDocumentService(
    uploadedDocumentRepository,
    courtAppearanceRepository,
    serviceUserService,
    documentManagementApiClient,
  )

  @Test
  fun `should unlink and link documents correctly`() {
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")

    val appearance = generateCourtAppearance("REFERENCE1", EntityStatus.ACTIVE, courtCase)
    val documentUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
    val documentsToUnlink = listOf(
      UploadedDocumentEntity(
        documentUuid = UUID.randomUUID(),
        appearance = appearance,
        documentType = "TYPE1",
        fileName = "file1.pdf",
        createdBy = "U",
      ),
    )
    every {
      uploadedDocumentRepository.findAllByAppearanceUUIDAndDocumentUuidNotIn(
        appearance.appearanceUuid,
        documentUUIDs,
      )
    } returns documentsToUnlink
    every { serviceUserService.getUsername() } returns "test-user"
    documentUUIDs.forEach { uuid ->
      every { uploadedDocumentRepository.findByDocumentUuid(uuid) } returns UploadedDocumentEntity(
        documentUuid = UUID.randomUUID(),
        appearance = null,
        documentType = "TYPE1",
        fileName = "file2.pdf",
        createdBy = "U",
      )
    }

    every { uploadedDocumentRepository.save(any()) } answers { firstArg() as UploadedDocumentEntity }
    service.update(documentUUIDs, appearance)

    documentsToUnlink.forEach { _ ->
      verify {
        uploadedDocumentRepository.save(
          withArg { saved ->
            assertThat(saved.appearance).isNull()
            assertThat(saved.updatedBy).isEqualTo("test-user")
          },
        )
      }
    }
    documentUUIDs.forEach { uuid ->
      verify {
        uploadedDocumentRepository.save(
          withArg { saved ->
            assertThat(saved.appearance).isEqualTo(appearance)
            assertThat(saved.updatedBy).isEqualTo("test-user")
          },
        )
      }
    }
  }

  private fun generateCourtAppearance(
    caseReference: String,
    statusId: EntityStatus,
    courtCase: CourtCaseEntity,
  ): CourtAppearanceEntity = CourtAppearanceEntity(
    id = idInt++,
    appearanceUuid = UUID.randomUUID(),
    appearanceOutcome = null,
    courtCase = courtCase,
    courtCode = "C",
    courtCaseReference = caseReference,
    appearanceDate = LocalDate.now(),
    statusId = statusId,
    previousAppearance = null,
    warrantId = null,
    createdBy = "U",
    createdPrison = "P",
    warrantType = "W",
    appearanceCharges = mutableSetOf(),
    nextCourtAppearance = null,
    overallConvictionDate = null,
  )

}