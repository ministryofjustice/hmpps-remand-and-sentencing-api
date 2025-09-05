package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

class SentenceTypeUpdateServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val sentenceTypeRepository = mockk<SentenceTypeRepository>()
  private val sentenceHistoryRepository = mockk<SentenceHistoryRepository>()
  private val serviceUserService = mockk<ServiceUserService>()

  private lateinit var sentenceTypeUpdateService: SentenceTypeUpdateService

  private val courtCaseUuid = UUID.randomUUID()
  private val sentenceUuid = UUID.randomUUID()
  private val sdsTypeUuid = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    sentenceTypeUpdateService = SentenceTypeUpdateService(
      courtCaseRepository,
      sentenceTypeRepository,
      sentenceHistoryRepository,
      serviceUserService,
    )

    every { serviceUserService.getUsername() } returns "test-user"
    every { sentenceHistoryRepository.save(any()) } returnsArgument 0
  }

  @Test
  fun `throw IllegalStateException when court case is deleted`() {
    // Given
    val courtCase = createCourtCase(status = EntityStatus.DELETED)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceTypeId = sdsTypeUuid,
        ),
      ),
    )

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Court case with UUID $courtCaseUuid is deleted")
  }

  private fun createCourtCase(status: EntityStatus = EntityStatus.ACTIVE): CourtCaseEntity = CourtCaseEntity(
    id = 1,
    prisonerId = "PRISONER1",
    caseUniqueIdentifier = courtCaseUuid.toString(),
    createdBy = "test-user",
    createdPrison = null,
    statusId = status,
    entityStatus = status,
    legacyData = null,
  )
}
