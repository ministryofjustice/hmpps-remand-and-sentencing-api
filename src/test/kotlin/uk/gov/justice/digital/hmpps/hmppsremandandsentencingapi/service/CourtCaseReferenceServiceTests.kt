package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy.CourtCaseReferenceService
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class CourtCaseReferenceServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val objectMapper = jacksonObjectMapper()
  private val courtAppearanceRepository = mockk<CourtAppearanceRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val courtCaseReferenceService = CourtCaseReferenceService(courtCaseRepository, objectMapper, courtAppearanceRepository, serviceUserService)

  @Test
  fun `insert new active case references`() {
    val courtCase = CourtCaseEntity.placeholderEntity(prisonerId = "1", createdByUsername = "U", legacyData = null)

    val activeCourtAppearance = generateCourtAppearance("REFERENCE1", EntityStatus.ACTIVE, courtCase)
    courtCase.appearances = listOf(activeCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = objectMapper.treeToValue(courtCase.legacyData, CourtCaseLegacyData::class.java).caseReferences
    Assertions.assertThat(caseReferences).hasSize(1).extracting<String> { it.offenderCaseReference }.contains(activeCourtAppearance.courtCaseReference!!)
  }

  @Test
  fun `remove deleted case references`() {
    val oldCaseReference = "OLDCASEREFERENCE"
    val courtCase = CourtCaseEntity.placeholderEntity(prisonerId = "1", createdByUsername = "U", legacyData = generateLegacyData(listOf(oldCaseReference)))
    val deletedCourtAppearance = generateCourtAppearance(oldCaseReference, EntityStatus.DELETED, courtCase)
    courtCase.appearances = listOf(deletedCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = objectMapper.treeToValue(courtCase.legacyData, CourtCaseLegacyData::class.java).caseReferences
    Assertions.assertThat(caseReferences).hasSize(0).extracting<String> { it.offenderCaseReference }.doesNotContain(deletedCourtAppearance.courtCaseReference!!)
  }

  @Test
  fun `keep other case references not in appearance list (legacy NOMIS case references)`() {
    val oldCaseReference = "OLDCASEREFERENCE"
    val existingReferences = listOf("A", "B", "C", "D")
    val courtCase = CourtCaseEntity.placeholderEntity(prisonerId = "1", createdByUsername = "U", legacyData = generateLegacyData(existingReferences + oldCaseReference))
    val activeCourtAppearance = generateCourtAppearance("ANEWREFERENCE", EntityStatus.ACTIVE, courtCase)
    val deletedCourtAppearance = generateCourtAppearance("OLDCASEREFERENCE", EntityStatus.DELETED, courtCase)
    courtCase.appearances = listOf(activeCourtAppearance, deletedCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = objectMapper.treeToValue(courtCase.legacyData, CourtCaseLegacyData::class.java).caseReferences
    Assertions.assertThat(caseReferences).hasSize(5).extracting<String> { it.offenderCaseReference }.containsExactlyInAnyOrder("ANEWREFERENCE", *existingReferences.toTypedArray())
  }

  private fun generateLegacyData(caseReferences: List<String>): JsonNode = objectMapper.valueToTree<JsonNode>(CourtCaseLegacyData(caseReferences.map { CaseReferenceLegacyData(it, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)) }.toMutableList()))

  private fun generateCourtAppearance(caseReference: String, statusId: EntityStatus, courtCase: CourtCaseEntity): CourtAppearanceEntity = CourtAppearanceEntity(appearanceUuid = UUID.randomUUID(), lifetimeUuid = UUID.randomUUID(), appearanceOutcome = null, courtCase = courtCase, courtCode = "C", courtCaseReference = caseReference, appearanceDate = LocalDate.now(), statusId = statusId, previousAppearance = null, warrantId = null, createdByUsername = "U", createdPrison = "P", warrantType = "W", taggedBail = null, charges = mutableSetOf(), nextCourtAppearance = null, overallConvictionDate = null)
}
