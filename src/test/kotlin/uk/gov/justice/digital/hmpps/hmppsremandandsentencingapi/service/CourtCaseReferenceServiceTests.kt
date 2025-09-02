package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy.CourtCaseReferenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CourtCaseReferenceServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val courtAppearanceRepository = mockk<CourtAppearanceRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val courtAppearanceHistoryRepository = mockk<CourtAppearanceHistoryRepository>()
  private val courtCaseHistoryRepository = mockk<CourtCaseHistoryRepository>()
  private val courtCaseReferenceService = CourtCaseReferenceService(
    courtCaseRepository,
    courtAppearanceRepository,
    serviceUserService,
    courtAppearanceHistoryRepository,
    courtCaseHistoryRepository,
  )
  private var idInt: Int = 0

  @Test
  fun `insert new active case references`() {
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")

    val activeCourtAppearance = generateCourtAppearance("REFERENCE1", EntityStatus.ACTIVE, courtCase)
    courtCase.appearances = setOf(activeCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    every { courtCaseHistoryRepository.save(any()) } returns mockk()
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences).hasSize(1).extracting<String> { it.offenderCaseReference }
      .contains(activeCourtAppearance.courtCaseReference!!)
  }

  @Test
  fun `remove deleted case references`() {
    val oldCaseReference = "OLDCASEREFERENCE"
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")
    courtCase.legacyData = generateLegacyData(listOf(oldCaseReference))
    val deletedCourtAppearance = generateCourtAppearance(oldCaseReference, EntityStatus.DELETED, courtCase)
    courtCase.appearances = setOf(deletedCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    every { courtCaseHistoryRepository.save(any()) } returns mockk()
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences).hasSize(0).extracting<String> { it.offenderCaseReference }
      .doesNotContain(deletedCourtAppearance.courtCaseReference!!)
  }

  @Test
  fun `keep other case references not in appearance list (legacy NOMIS case references)`() {
    val oldCaseReference = "OLDCASEREFERENCE"
    val existingReferences = listOf("A", "B", "C", "D")
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")
    courtCase.legacyData = generateLegacyData(existingReferences + oldCaseReference)
    val activeCourtAppearance = generateCourtAppearance("ANEWREFERENCE", EntityStatus.ACTIVE, courtCase)
    val deletedCourtAppearance = generateCourtAppearance("OLDCASEREFERENCE", EntityStatus.DELETED, courtCase)
    courtCase.appearances = setOf(activeCourtAppearance, deletedCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    every { courtCaseHistoryRepository.save(any()) } returns mockk()
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences).hasSize(5).extracting<String> { it.offenderCaseReference }
      .containsExactlyInAnyOrder("ANEWREFERENCE", *existingReferences.toTypedArray())
  }

  @Test
  fun `remove DPS legacy refs not on active appearances but keep NOMIS ones`() {
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")

    val nomisRef = "NOMIS-CASE-REFERENCE"
    val dpsRef = "DPS-CASE-REFERENCE"
    courtCase.legacyData = CourtCaseLegacyData(
      mutableListOf(
        CaseReferenceLegacyData(nomisRef, LocalDateTime.now(), EventSource.NOMIS),
        CaseReferenceLegacyData(dpsRef, LocalDateTime.now(), EventSource.DPS),
      ),
      1L,
    )

    val active = generateCourtAppearance("ACTIVE-CASE-REF", EntityStatus.ACTIVE, courtCase)
    courtCase.appearances = setOf(active)

    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    every { courtCaseHistoryRepository.save(any()) } returns mockk()

    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)

    val refs = courtCase.legacyData!!.caseReferences.map { it.offenderCaseReference }
    Assertions.assertThat(refs).containsExactlyInAnyOrder("ACTIVE-CASE-REF", nomisRef)
  }

  @Test
  fun `keep old case reference when another active appearance still uses it `() {
    val oldRef = "OLDCASEREFERENCE"
    val newRef = "ANEWREFERENCE"
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")
    courtCase.legacyData = generateLegacyData(listOf(oldRef))

    val stillUsingOld = generateCourtAppearance(oldRef, EntityStatus.ACTIVE, courtCase)
    val editedToNew = generateCourtAppearance(newRef, EntityStatus.ACTIVE, courtCase)
    courtCase.appearances = setOf(stillUsingOld, editedToNew)

    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    every { courtCaseHistoryRepository.save(any()) } returns mockk()

    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)

    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences)
      .hasSize(2)
      .extracting<String> { it.offenderCaseReference }
      .containsExactlyInAnyOrder(oldRef, newRef)
  }

  private fun generateLegacyData(caseReferences: List<String>): CourtCaseLegacyData = CourtCaseLegacyData(caseReferences.map { CaseReferenceLegacyData(it, LocalDateTime.now()) }.toMutableList(), 1L)

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
