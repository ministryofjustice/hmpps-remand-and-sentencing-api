package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
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
  private val courtCaseReferenceService = CourtCaseReferenceService(courtCaseRepository, courtAppearanceRepository, serviceUserService, courtAppearanceHistoryRepository)
  private var idInt: Int = 0
//
//
//    @Test
//    fun `chargesByConsecutiveToLast should produce 1,2,3,4 for chain 4→3→2→1 (but does not)`() {
//      // Build a simple chain using the provided DTOs:
//      // 1 (base), 2 -> 1, 3 -> 2, 4 -> 3
//      val c1 = charge(ref = "1", to = null)
//      val c2 = charge(ref = "2", to = "1")
//      val c3 = charge(ref = "3", to = "2")
//      val c4 = charge(ref = "4", to = "3")
//
//      // Adversarial initial order; a stable sort is allowed to keep 4 before 2 because compare(4,2)=0
//      val initial = listOf(c4, c2, c3, c1)
//
//      // Sort with the comparator under test
//      val sorted = initial.sortedWith(::chargesByConsecutiveToLast)
//
//      val actual = sorted.map { it.sentence!!.sentenceReference }
//      // ✅ What we *expect* from a correct (transitive) order:
//      // parents before dependents ⇒ 1, 2, 3, 4
//      // ❌ This assertion should FAIL with the current comparator. actual ["1", "4", "2", "3"]
//      Assertions.assertThat(actual).containsExactly("1", "2", "3", "4")
//    }
//
//    /** Minimal factory using DpsDataCreator + overriding the sentence bits we care about */
//    private fun charge(ref: String, to: String?): CreateCharge =
//      DpsDataCreator.dpsCreateCharge(
//        sentence = DpsDataCreator.dpsCreateSentence(
//          chargeNumber = ref,
//          sentenceServeType = if (to == null) "FORTHWITH" else "CONSECUTIVE",
//          sentenceReference = ref,                    // use count as the reference for clarity
//          consecutiveToSentenceReference = to,
//          consecutiveToSentenceUuid = null
//        )
//      )
//
//
//
//  @Test
//  fun `chargesByConsecutiveToLast is not transitive for indirect dependencies (4 -gt 3, 3 -gt 2, but 4 !gt 2)`() {
//    // Build a simple chain: 1 (base), 2 -> 1, 3 -> 2, 4 -> 3
//    val c1 = createCharge("1", consecutiveTo = null)
//    val c2 = createCharge("2", consecutiveTo = "1")
//    val c3 = createCharge("3", consecutiveTo = "2")
//    val c4 = createCharge("4", consecutiveTo = "3")
//
//    // Sanity: direct edges compare as expected
//    Assertions.assertThat(chargesByConsecutiveToLast(c4, c3)).isEqualTo(1)  // 4 must be after 3
//    Assertions.assertThat(chargesByConsecutiveToLast(c3, c2)).isEqualTo(1)  // 3 must be after 2
//
//    // ❌ Indirect edge (4 vs 2) is treated as "equal"/no preference
//    // This breaks transitivity required by a total order and can yield bad sort results.
//    Assertions.assertThat(chargesByConsecutiveToLast(c4, c2)).isEqualTo(0)
//
//    // Illustrate how a sort can keep a bad relative order due to the 0 above (stable sort)
//    val initial = listOf(c4, c2, c3, c1) // adversarial starting order
//    val sorted = initial.sortedWith(::chargesByConsecutiveToLast)
//
//    // Required topological order is 1,2,3,4 — but comparator allows 4 to remain before 2.
//    val byRef = sorted.map { it.sentence!!.sentenceReference }
//    // 1 must be before 2 and 3; those usually hold, but 4 can illegally sit before 2.
//    Assertions.assertThat(byRef.indexOf("4")).isLessThan(byRef.indexOf("2"))
//  }
//
//  /** Minimal builders using the provided DTOs */
//  private fun createCharge(ref: String, consecutiveTo: String?): CreateCharge =
//    CreateCharge(
//      appearanceUuid = null,
//      offenceCode = "X",
//      offenceStartDate = LocalDate.now(),
//      offenceEndDate = null,
//      outcomeUuid = null,
//      terrorRelated = null,
//      prisonId = "P",
//      legacyData = null,
//      sentence = CreateSentence(
//        sentenceUuid = null,
//        chargeNumber = ref,                         // just reuse ref for simplicity
//        periodLengths = emptyList(),                // not relevant for this test
//        sentenceServeType = if (consecutiveTo == null) "FORTHWITH" else "CONSECUTIVE",
//        consecutiveToSentenceUuid = null,
//        sentenceTypeId = null,
//        convictionDate = null,
//        fineAmount = null,
//        prisonId = null,
//        sentenceReference = ref,
//        consecutiveToSentenceReference = consecutiveTo
//      )
//    )
//
//  /** The comparator under test (same logic as provided) */
//  private fun chargesByConsecutiveToLast(a: CreateCharge, b: CreateCharge): Int {
//    val aRef = a.sentence?.sentenceReference
//    val bRef = b.sentence?.sentenceReference
//    val aTo  = a.sentence?.consecutiveToSentenceReference
//    val bTo  = b.sentence?.consecutiveToSentenceReference
//
//    return when {
//      aTo != null && aTo == bRef -> 1
//      bTo != null && bTo == aRef -> -1
//      aTo == null && bTo != null -> -1
//      aTo != null && bTo == null -> 1
//      else -> 0
//    }
//  }

  // ...existing tests...

  @Test
  fun `orderChargesTopologically sorts linear chain 4 -gt 3 -gt 2 -gt 1 into 1,2,3,4`() {
    // 1 (base), 2->1, 3->2, 4->3
    val c1 = charge(ref = "1", to = null)
    val c2 = charge(ref = "2", to = "1")
    val c3 = charge(ref = "3", to = "2")
    val c4 = charge(ref = "4", to = "3")

    val adversarial = listOf(c4, c2, c3, c1)

    val sorted = orderChargesTopologically(adversarial)

    val actual = sorted.map { it.sentence!!.sentenceReference }
    assertThat(actual).containsExactly("1", "2", "3", "4")
  }

  @Test
  fun `orderChargesTopologically throws on cycle`() {
    // 1->2 and 2->1 (cycle)
    val c1 = charge("1", "2")
    val c2 = charge("2", "1")

    assertThatThrownBy { orderChargesTopologically(listOf(c1, c2)) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("Cycle")
  }

  // --- helpers (minimal DTO builders) ---

  private fun charge(ref: String, to: String?): CreateCharge = CreateCharge(
    appearanceUuid = null,
    offenceCode = "X",
    offenceStartDate = LocalDate.now(),
    offenceEndDate = null,
    outcomeUuid = null,
    terrorRelated = null,
    prisonId = "P",
    legacyData = null,
    sentence = CreateSentence(
      sentenceUuid = null,
      chargeNumber = ref, // reuse ref for clarity
      periodLengths = emptyList(),
      sentenceServeType = if (to == null) "FORTHWITH" else "CONSECUTIVE",
      consecutiveToSentenceUuid = null,
      sentenceTypeId = null,
      convictionDate = null,
      fineAmount = null,
      prisonId = null,
      sentenceReference = ref,
      consecutiveToSentenceReference = to,
    ),
  )

  private fun orderChargesTopologically(charges: List<CreateCharge>): List<CreateCharge> {
    // Build lookup by sentenceReference (treat as String)
    val byRef: Map<String, CreateCharge> =
      charges.mapNotNull { c -> c.sentence?.sentenceReference?.let { it to c } }.toMap()

    // helper sets
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()
    val result = mutableListOf<CreateCharge>()

    fun visit(c: CreateCharge) {
      val ref = c.sentence?.sentenceReference
      if (ref == null) { // if no reference, just append as base
        result += c
        return
      }
      if (ref in visited) return
      if (ref in visiting) {
        throw IllegalStateException("Cycle detected involving sentenceReference=$ref")
      }
      visiting += ref

      // ensure parent (consecutiveTo) is processed first, if present
      c.sentence.consecutiveToSentenceReference
        ?.let { parentRef -> byRef[parentRef] }
        ?.let { parent -> visit(parent) }

      visiting.remove(ref)
      visited.add(ref)
      result += c
    }

    charges.forEach { visit(it) }
    return result
  }

  @Test
  fun `insert new active case references`() {
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")

    val activeCourtAppearance = generateCourtAppearance("REFERENCE1", EntityStatus.ACTIVE, courtCase)
    courtCase.appearances = setOf(activeCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences).hasSize(1).extracting<String> { it.offenderCaseReference }.contains(activeCourtAppearance.courtCaseReference!!)
  }

  @Test
  fun `remove deleted case references`() {
    val oldCaseReference = "OLDCASEREFERENCE"
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "U")
    courtCase.legacyData = generateLegacyData(listOf(oldCaseReference))
    val deletedCourtAppearance = generateCourtAppearance(oldCaseReference, EntityStatus.DELETED, courtCase)
    courtCase.appearances = setOf(deletedCourtAppearance)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCase.caseUniqueIdentifier) } returns courtCase
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences).hasSize(0).extracting<String> { it.offenderCaseReference }.doesNotContain(deletedCourtAppearance.courtCaseReference!!)
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
    courtCaseReferenceService.updateCourtCaseReferences(courtCase.caseUniqueIdentifier)
    val caseReferences = courtCase.legacyData!!.caseReferences
    Assertions.assertThat(caseReferences).hasSize(5).extracting<String> { it.offenderCaseReference }.containsExactlyInAnyOrder("ANEWREFERENCE", *existingReferences.toTypedArray())
  }

  private fun generateLegacyData(caseReferences: List<String>): CourtCaseLegacyData = CourtCaseLegacyData(caseReferences.map { CaseReferenceLegacyData(it, LocalDateTime.now()) }.toMutableList(), 1L)

  private fun generateCourtAppearance(caseReference: String, statusId: EntityStatus, courtCase: CourtCaseEntity): CourtAppearanceEntity = CourtAppearanceEntity(id = idInt++, appearanceUuid = UUID.randomUUID(), appearanceOutcome = null, courtCase = courtCase, courtCode = "C", courtCaseReference = caseReference, appearanceDate = LocalDate.now(), statusId = statusId, previousAppearance = null, warrantId = null, createdBy = "U", createdPrison = "P", warrantType = "W", appearanceCharges = mutableSetOf(), nextCourtAppearance = null, overallConvictionDate = null)
}
