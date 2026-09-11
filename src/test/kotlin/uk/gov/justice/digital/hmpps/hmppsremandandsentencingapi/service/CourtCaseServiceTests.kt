package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateCourtCaseStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import java.util.UUID

class CourtCaseServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val courtAppearanceService = mockk<CourtAppearanceService>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val fixManyChargesToSentenceService = mockk<FixManyChargesToSentenceService>()
  private val courtCaseHistoryRepository = mockk<CourtCaseHistoryRepository>()

  private lateinit var courtCaseService: CourtCaseService

  private val caseUniqueIdentifier = UUID.randomUUID().toString()

  @BeforeEach
  fun setUp() {
    courtCaseService = CourtCaseService(
      courtCaseRepository,
      courtAppearanceService,
      serviceUserService,
      fixManyChargesToSentenceService,
      courtCaseHistoryRepository,
    )

    every { serviceUserService.getUsername() } returns "test-user"
    every { courtCaseHistoryRepository.save(any()) } returnsArgument 0
  }

  @Test
  fun `updateCourtCaseStatus marks a court case as inactive with a reason`() {
    val courtCase = createCourtCase()
    every { courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) } returns courtCase

    val eventsToEmit = courtCaseService.updateCourtCaseStatus(
      caseUniqueIdentifier,
      UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Duplicate case"),
    )

    assertThat(courtCase.statusId).isEqualTo(CourtCaseEntityStatus.INACTIVE)
    assertThat(courtCase.reason).isEqualTo("Duplicate case")
    assertThat(courtCase.updatedBy).isEqualTo("test-user")
    assertThat(eventsToEmit).hasSize(1)
    verify { courtCaseHistoryRepository.save(any()) }
  }

  @Test
  fun `updateCourtCaseStatus marks a court case as active`() {
    val courtCase = createCourtCase(status = CourtCaseEntityStatus.INACTIVE, reason = "Duplicate case")
    every { courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) } returns courtCase

    courtCaseService.updateCourtCaseStatus(
      caseUniqueIdentifier,
      UpdateCourtCaseStatus(status = CourtCaseEntityStatus.ACTIVE, reason = null),
    )

    assertThat(courtCase.statusId).isEqualTo(CourtCaseEntityStatus.ACTIVE)
    assertThat(courtCase.reason).isNull()
  }

  @Test
  fun `updateCourtCaseStatus throws when court case cannot be found`() {
    every { courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) } returns null

    assertThatThrownBy {
      courtCaseService.updateCourtCaseStatus(
        caseUniqueIdentifier,
        UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Duplicate case"),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun `updateCourtCaseStatus rejects statuses other than ACTIVE or INACTIVE`() {
    val courtCase = createCourtCase()
    every { courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) } returns courtCase

    assertThatThrownBy {
      courtCaseService.updateCourtCaseStatus(
        caseUniqueIdentifier,
        UpdateCourtCaseStatus(status = CourtCaseEntityStatus.MERGED, reason = null),
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
  }

  private fun createCourtCase(status: CourtCaseEntityStatus = CourtCaseEntityStatus.ACTIVE, reason: String? = null): CourtCaseEntity = CourtCaseEntity(
    prisonerId = "PRISONER1",
    caseUniqueIdentifier = caseUniqueIdentifier,
    createdBy = "test-user",
    createdPrison = null,
    statusId = status,
    reason = reason,
    legacyData = null,
  )
}
