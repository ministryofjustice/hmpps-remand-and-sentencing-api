package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCaseSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class RecallServiceMergeTest {

  private val recallRepository: RecallRepository = mockk(relaxed = true)
  private val recallSentenceRepository: RecallSentenceRepository = mockk(relaxed = true)
  private val recallTypeRepository: RecallTypeRepository = mockk(relaxed = true)
  private val sentenceRepository: SentenceRepository = mockk(relaxed = true)
  private val sentenceService: SentenceService = mockk(relaxed = true)
  private val recallHistoryRepository: RecallHistoryRepository = mockk(relaxed = true)
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository = mockk(relaxed = true)
  private val adjustmentsApiClient: AdjustmentsApiClient = mockk(relaxed = true)
  private val sentenceHistoryRepository: SentenceHistoryRepository = mockk(relaxed = true)
  private val serviceUserService: ServiceUserService = mockk(relaxed = true)
  private val courtCaseRepository: CourtCaseRepository = mockk()
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService = mockk()

  private val service = RecallService(
    recallRepository,
    recallSentenceRepository,
    recallTypeRepository,
    sentenceRepository,
    sentenceService,
    recallHistoryRepository,
    recallSentenceHistoryRepository,
    adjustmentsApiClient,
    sentenceHistoryRepository,
    serviceUserService,
    courtCaseRepository,
    fixManyChargesToSentenceService,
  )

  @Nested
  inner class MergeAndSortRecallableCourtCases {
    @Test
    fun `dedupes duplicate sentences, merges court cases, and keeps non duplicates`() {
      val dupKeyOffence = "OFF1"
      val dupStart = LocalDate.of(2020, 1, 1)
      val dupSentenceDate = LocalDate.of(2020, 2, 1)

      val olderDup = recallableSentence(
        uuid = UUID.randomUUID(),
        offenceCode = dupKeyOffence,
        offenceStartDate = dupStart,
        sentenceDate = dupSentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 1, 10, 0),
      )
      val newerDup = olderDup.copy(
        sentenceUuid = UUID.randomUUID(),
        createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
      )

      val uniqueA = recallableSentence(UUID.randomUUID(), "A1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0))
      val uniqueB = recallableSentence(UUID.randomUUID(), "B1", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0))

      val case1 = recallableCourtCase(
        uuid = "CASE-1",
        courtCode = "C1",
        appearanceDate = LocalDate.of(2024, 1, 10),
        sentences = listOf(olderDup, uniqueA),
      )
      val case2 = recallableCourtCase(
        uuid = "CASE-2",
        courtCode = "C1",
        appearanceDate = LocalDate.of(2024, 1, 5),
        sentences = listOf(newerDup, uniqueB),
      )

      val result = service.mergeAndSortCourtCases(listOf(case1, case2))

      assertThat(result).hasSize(1)

      val merged = result.single()
      assertThat(merged.courtCode).isEqualTo("C1")
      assertThat(merged.courtCaseUuid).isEqualTo("CASE-2")
      assertThat(merged.sentences.map { it.offenceCode }).containsExactlyInAnyOrder(dupKeyOffence, "A1", "B1")
      assertThat(merged.sentences.filter { it.offenceCode == dupKeyOffence }).hasSize(1)
      assertThat(merged.sentences.single { it.offenceCode == dupKeyOffence }.createdAt)
        .isEqualTo(LocalDateTime.of(2020, 2, 2, 10, 0))
    }

    @Test
    fun `drops court cases that become empty after dedupe`() {
      val dupSentenceDate = LocalDate.of(2020, 2, 1)

      val olderDup = recallableSentence(
        uuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = LocalDate.of(2020, 1, 1),
        sentenceDate = dupSentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 1, 10, 0),
      )
      val newerDupOnly = recallableSentence(
        uuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = LocalDate.of(2020, 1, 1),
        sentenceDate = dupSentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
      )

      val winnerCase = recallableCourtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(olderDup))
      val loserOnlyCase = recallableCourtCase("CASE-2", "C1", LocalDate.of(2024, 1, 5), listOf(newerDupOnly))

      val result = service.mergeAndSortCourtCases(listOf(winnerCase, loserOnlyCase))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-2")
    }

    @Test
    fun `sorts merged cases by appearanceDate desc`() {
      val s1 = recallableSentence(UUID.randomUUID(), "X1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))
      val s2 = recallableSentence(UUID.randomUUID(), "Y1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 10, 0))

      val newer = recallableCourtCase("CASE-NEW", "C1", LocalDate.of(2024, 2, 1), listOf(s1))
      val older = recallableCourtCase("CASE-OLD", "C2", LocalDate.of(2024, 1, 1), listOf(s2))

      val result = service.mergeAndSortCourtCases(listOf(older, newer))

      assertThat(result.map { it.courtCaseUuid }).containsExactly("CASE-NEW", "CASE-OLD")
    }

    @Test
    fun `merges 4 court cases with shared duplicate into latest duplicate sentence court case`() {
      val earliestDup1 = recallableSentence(UUID.randomUUID(), "OFF-DUPLICATE", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))
      val dup2 = earliestDup1.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 2, 10, 0))
      val dup3 = earliestDup1.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 3, 10, 0))
      val dup4 = earliestDup1.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 4, 10, 0))

      val unique1 = recallableSentence(UUID.randomUUID(), "U1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0))
      val unique2 = recallableSentence(UUID.randomUUID(), "U2", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0))
      val unique3 = recallableSentence(UUID.randomUUID(), "U3", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 2, 1), LocalDateTime.of(2017, 2, 1, 9, 0))
      val unique4 = recallableSentence(UUID.randomUUID(), "U4", LocalDate.of(2016, 1, 1), LocalDate.of(2016, 2, 1), LocalDateTime.of(2016, 2, 1, 9, 0))

      val case1 = recallableCourtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(earliestDup1, unique1))
      val case2 = recallableCourtCase("CASE-2", "C1", LocalDate.of(2024, 1, 9), listOf(dup2, unique2))
      val case3 = recallableCourtCase("CASE-3", "C1", LocalDate.of(2024, 1, 8), listOf(dup3, unique3))
      val case4 = recallableCourtCase("CASE-4", "C1", LocalDate.of(2024, 1, 7), listOf(dup4, unique4))

      val result = service.mergeAndSortCourtCases(listOf(case4, case2, case3, case1))

      assertThat(result).hasSize(1)
      val merged = result.single()

      assertThat(merged.courtCaseUuid).isEqualTo("CASE-4")
      assertThat(merged.sentences.map { it.offenceCode })
        .containsExactlyInAnyOrder("OFF-DUPLICATE", "U1", "U2", "U3", "U4")

      assertThat(merged.sentences.filter { it.offenceCode == "OFF-DUPLICATE" }).hasSize(1)
      assertThat(merged.sentences.single { it.offenceCode == "OFF-DUPLICATE" }.createdAt)
        .isEqualTo(LocalDateTime.of(2020, 2, 4, 10, 0))
    }

    @Test
    fun `returns input unchanged when there are no duplicates across 4 court cases`() {
      val c1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(recallableSentence(UUID.randomUUID(), "O1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))),
      )
      val c2 = recallableCourtCase(
        "CASE-2",
        "C2",
        LocalDate.of(2024, 1, 9),
        listOf(recallableSentence(UUID.randomUUID(), "O2", LocalDate.of(2020, 1, 2), LocalDate.of(2020, 2, 2), LocalDateTime.of(2020, 2, 2, 10, 0))),
      )
      val c3 = recallableCourtCase(
        "CASE-3",
        "C3",
        LocalDate.of(2024, 1, 8),
        listOf(recallableSentence(UUID.randomUUID(), "O3", LocalDate.of(2020, 1, 3), LocalDate.of(2020, 2, 3), LocalDateTime.of(2020, 2, 3, 10, 0))),
      )
      val c4 = recallableCourtCase(
        "CASE-4",
        "C4",
        LocalDate.of(2024, 1, 7),
        listOf(recallableSentence(UUID.randomUUID(), "O4", LocalDate.of(2020, 1, 4), LocalDate.of(2020, 2, 4), LocalDateTime.of(2020, 2, 4, 10, 0))),
      )

      val result = service.mergeAndSortCourtCases(listOf(c3, c1, c4, c2))
      assertThat(result).isEqualTo(listOf(c1, c2, c3, c4))
    }

    @Test
    fun `merges duplicate cases and leaves non duplicate cases unchanged`() {
      val dupStart = LocalDate.of(2020, 1, 1)
      val dupSentenceDate = LocalDate.of(2020, 2, 1)

      val olderDup = recallableSentence(
        UUID.randomUUID(),
        "OFF-DUP",
        dupStart,
        dupSentenceDate,
        LocalDateTime.of(2020, 2, 1, 10, 0),
      )
      val newerDup = olderDup.copy(
        sentenceUuid = UUID.randomUUID(),
        createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
      )

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(
          olderDup,
          recallableSentence(UUID.randomUUID(), "U1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0)),
        ),
      )
      val case2 = recallableCourtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(
          newerDup,
          recallableSentence(UUID.randomUUID(), "U2", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0)),
          recallableSentence(UUID.randomUUID(), "U3", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 2, 1), LocalDateTime.of(2017, 2, 1, 9, 0)),
        ),
      )
      val case3 = recallableCourtCase(
        "CASE-3",
        "C3",
        LocalDate.of(2024, 1, 8),
        listOf(
          recallableSentence(UUID.randomUUID(), "O3", LocalDate.of(2020, 1, 3), LocalDate.of(2020, 2, 3), LocalDateTime.of(2020, 2, 3, 10, 0)),
        ),
      )
      val case4 = recallableCourtCase(
        "CASE-4",
        "C4",
        LocalDate.of(2024, 1, 7),
        listOf(
          recallableSentence(UUID.randomUUID(), "O4", LocalDate.of(2020, 1, 4), LocalDate.of(2020, 2, 4), LocalDateTime.of(2020, 2, 4, 10, 0)),
        ),
      )

      val result = service.mergeAndSortCourtCases(listOf(case4, case2, case3, case1))

      assertThat(result.map { it.courtCaseUuid }).containsExactly("CASE-2", "CASE-3", "CASE-4")

      val resultMergedCase2 = result[0]
      assertThat(resultMergedCase2.courtCaseUuid).isEqualTo("CASE-2")
      assertThat(resultMergedCase2.sentences.map { it.offenceCode }).containsExactlyInAnyOrder("OFF-DUP", "U1", "U2", "U3")

      val resultCase3 = result[1]
      assertThat(resultCase3.courtCaseUuid).isEqualTo("CASE-3")
      assertThat(resultCase3.sentences.map { it.offenceCode }).containsExactly("O3")

      val resultCase4 = result[2]
      assertThat(resultCase4.courtCaseUuid).isEqualTo("CASE-4")
      assertThat(resultCase4.sentences.map { it.offenceCode }).containsExactly("O4")
    }

    @Test
    fun `does not treat duplicate key within the same court case as a cross case duplicate`() {
      val dup1 = recallableSentence(
        UUID.randomUUID(),
        "OFF1",
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 2, 1),
        LocalDateTime.of(2020, 2, 1, 10, 0),
      )
      val dup2 = dup1.copy(
        sentenceUuid = UUID.randomUUID(),
        createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
      )

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(dup1, dup2),
      )
      val case2 = recallableCourtCase(
        "CASE-2",
        "C2",
        LocalDate.of(2024, 1, 9),
        listOf(recallableSentence(UUID.randomUUID(), "O2", LocalDate.of(2020, 1, 2), LocalDate.of(2020, 2, 2), LocalDateTime.of(2020, 2, 2, 9, 0))),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).isEqualTo(listOf(case1, case2))
    }

    @Test
    fun `does not merge when duplicate sentence fields match but court code differs`() {
      val sentenceDate = LocalDate.of(2020, 2, 1)
      val startDate = LocalDate.of(2020, 1, 1)

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(recallableSentence(UUID.randomUUID(), "OFF1", startDate, sentenceDate, LocalDateTime.of(2020, 2, 1, 10, 0))),
      )
      val case2 = recallableCourtCase(
        "CASE-2",
        "C2",
        LocalDate.of(2024, 1, 9),
        listOf(recallableSentence(UUID.randomUUID(), "OFF1", startDate, sentenceDate, LocalDateTime.of(2020, 2, 2, 10, 0))),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).isEqualTo(listOf(case1, case2))
    }

    @Test
    fun `merges transitively linked court cases`() {
      val keyAStart = LocalDate.of(2020, 1, 1)
      val keyADate = LocalDate.of(2020, 2, 1)
      val keyBStart = LocalDate.of(2020, 3, 1)
      val keyBDate = LocalDate.of(2020, 4, 1)

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(
          recallableSentence(UUID.randomUUID(), "A", keyAStart, keyADate, LocalDateTime.of(2020, 2, 1, 10, 0)),
          recallableSentence(UUID.randomUUID(), "U1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0)),
        ),
      )
      val case2 = recallableCourtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(
          recallableSentence(UUID.randomUUID(), "A", keyAStart, keyADate, LocalDateTime.of(2020, 2, 2, 10, 0)),
          recallableSentence(UUID.randomUUID(), "B", keyBStart, keyBDate, LocalDateTime.of(2020, 4, 1, 10, 0)),
          recallableSentence(UUID.randomUUID(), "U2", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0)),
        ),
      )
      val case3 = recallableCourtCase(
        "CASE-3",
        "C1",
        LocalDate.of(2024, 1, 8),
        listOf(
          recallableSentence(UUID.randomUUID(), "B", keyBStart, keyBDate, LocalDateTime.of(2020, 4, 2, 10, 0)),
          recallableSentence(UUID.randomUUID(), "U3", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 2, 1), LocalDateTime.of(2017, 2, 1, 9, 0)),
        ),
      )

      val result = service.mergeAndSortCourtCases(listOf(case3, case2, case1))

      assertThat(result).hasSize(1)
      val merged = result.single()
      assertThat(merged.courtCaseUuid).isEqualTo("CASE-3")
      assertThat(merged.sentences.map { it.offenceCode })
        .containsExactlyInAnyOrder("A", "B", "U1", "U2", "U3")
    }

    @Test
    fun `uses case uuid as tiebreaker when duplicate winners have the same created at`() {
      val createdAt = LocalDateTime.of(2020, 2, 1, 10, 0)

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(recallableSentence(UUID.randomUUID(), "OFF1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), createdAt)),
      )
      val case2 = recallableCourtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(recallableSentence(UUID.randomUUID(), "OFF1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), createdAt)),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-2")
    }

    @Test
    fun `does not merge when duplicate sentence fields match but period lengths differ`() {
      val sentenceDate = LocalDate.of(2020, 2, 1)
      val startDate = LocalDate.of(2020, 1, 1)

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(
          recallableSentence(
            uuid = UUID.randomUUID(),
            offenceCode = "OFF1",
            offenceStartDate = startDate,
            sentenceDate = sentenceDate,
            createdAt = LocalDateTime.of(2020, 2, 1, 10, 0),
          ).copy(
            periodLengths = listOf(periodLength(years = 1)),
          ),
        ),
      )

      val case2 = recallableCourtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(
          recallableSentence(
            uuid = UUID.randomUUID(),
            offenceCode = "OFF1",
            offenceStartDate = startDate,
            sentenceDate = sentenceDate,
            createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
          ).copy(
            periodLengths = listOf(periodLength(years = 2)),
          ),
        ),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).isEqualTo(listOf(case1, case2))
    }

    @Test
    fun `merges when duplicate sentence fields and period lengths match`() {
      val sentenceDate = LocalDate.of(2020, 2, 1)
      val startDate = LocalDate.of(2020, 1, 1)

      val matchingPeriodLengths = listOf(periodLength(years = 1))

      val olderSentence = recallableSentence(
        uuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = startDate,
        sentenceDate = sentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 1, 10, 0),
      ).copy(periodLengths = matchingPeriodLengths)

      val newerSentence = recallableSentence(
        uuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = startDate,
        sentenceDate = sentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
      ).copy(periodLengths = matchingPeriodLengths)

      val case1 = recallableCourtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(olderSentence),
      )
      val case2 = recallableCourtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(newerSentence),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-2")
      assertThat(result.single().sentences).hasSize(1)
      assertThat(result.single().sentences.single().sentenceUuid).isEqualTo(newerSentence.sentenceUuid)
    }
  }

  @Nested
  inner class GetRecallableCourtCasesMergeDuplicateCourtCasesFlag {

    @Test
    fun `returns normal sorted cases when merge duplicate court cases is false`() {
      val case1 = recallableCourtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(recallableSentence(UUID.randomUUID(), "O1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))))
      val case2 = recallableCourtCase("CASE-2", "C1", LocalDate.of(2024, 1, 9), listOf(recallableSentence(UUID.randomUUID(), "O2", LocalDate.of(2020, 1, 2), LocalDate.of(2020, 2, 2), LocalDateTime.of(2020, 2, 2, 10, 0))))

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).isEqualTo(listOf(case1, case2))
    }

    @Test
    fun `returns merged cases when merge duplicate court cases is true`() {
      val olderDup = recallableSentence(UUID.randomUUID(), "OFF1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))
      val newerDup = olderDup.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 2, 10, 0))

      val case1 = recallableCourtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(olderDup))
      val case2 = recallableCourtCase("CASE-2", "C1", LocalDate.of(2024, 1, 9), listOf(newerDup))

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-2")
    }
  }

  @Nested
  inner class GetRecallableCourtCasesCreatedAtMapping {
    @Test
    fun `uses posted date when it is earlier than created at`() {
      val createdAt = ZonedDateTime.parse("2020-02-07T10:00:00Z")
      val postedDate = "2020-02-06T12:48:05.692411"

      every { courtCaseRepository.findSentencedCourtCasesByPrisonerId("A1234BC") } returns listOf(courtCaseWithSentence(createdAt, postedDate))
      every { fixManyChargesToSentenceService.fixCourtCaseSentences(any()) } returns mutableSetOf()

      val result = service.getRecallableCourtCases("A1234BC")

      assertThat(result.record.cases.single().sentences.single().createdAt).isEqualTo(LocalDateTime.parse("2020-02-06T12:48:05.692411"))
    }

    @Test
    fun `uses created at when it is earlier than posted date`() {
      val createdAt = ZonedDateTime.parse("2020-02-05T10:00:00Z")
      val postedDate = "2020-02-06T12:48:05.692411"

      every { courtCaseRepository.findSentencedCourtCasesByPrisonerId("A1234BC") } returns listOf(courtCaseWithSentence(createdAt, postedDate))
      every { fixManyChargesToSentenceService.fixCourtCaseSentences(any()) } returns mutableSetOf()

      val result = service.getRecallableCourtCases("A1234BC")

      assertThat(result.record.cases.single().sentences.single().createdAt).isEqualTo(LocalDateTime.parse("2020-02-05T10:00:00"))
    }

    @Test
    fun `uses created at when posted date is absent`() {
      val createdAt = ZonedDateTime.parse("2020-02-05T10:00:00Z")

      every { courtCaseRepository.findSentencedCourtCasesByPrisonerId("A1234BC") } returns listOf(courtCaseWithSentence(createdAt, null))
      every { fixManyChargesToSentenceService.fixCourtCaseSentences(any()) } returns mutableSetOf()

      val result = service.getRecallableCourtCases("A1234BC")

      assertThat(result.record.cases.single().sentences.single().createdAt).isEqualTo(LocalDateTime.parse("2020-02-05T10:00:00"))
    }
  }

  companion object {
    private fun recallableCourtCase(
      uuid: String,
      courtCode: String,
      appearanceDate: LocalDate,
      sentences: List<RecallableCourtCaseSentence>,
    ) = RecallableCourtCase(
      courtCaseUuid = uuid,
      reference = "REF-$uuid",
      courtCode = courtCode,
      status = CourtCaseEntityStatus.ACTIVE,
      isSentenced = sentences.isNotEmpty(),
      sentences = sentences,
      appearanceDate = appearanceDate,
      firstDayInCustody = null,
    )

    private fun recallableSentence(
      uuid: UUID,
      offenceCode: String?,
      offenceStartDate: LocalDate?,
      sentenceDate: LocalDate?,
      createdAt: LocalDateTime,
    ) = RecallableCourtCaseSentence(
      sentenceUuid = uuid,
      offenceCode = offenceCode,
      offenceStartDate = offenceStartDate,
      offenceEndDate = null,
      outcome = null,
      sentenceType = null,
      classification = null,
      systemOfRecord = "RAS",
      fineAmount = null,
      periodLengths = emptyList(),
      convictionDate = null,
      chargeLegacyData = null,
      countNumber = null,
      lineNumber = null,
      sentenceServeType = null,
      sentenceLegacyData = null,
      outcomeDescription = null,
      isRecallable = true,
      sentenceTypeUuid = UUID.randomUUID().toString(),
      sentenceDate = sentenceDate,
      consecutiveToSentenceUuid = null,
      createdAt = createdAt,
    )

    private fun courtCaseWithSentence(
      sentenceCreatedAt: ZonedDateTime,
      postedDate: String?,
    ): CourtCaseEntity {
      val courtCase = CourtCaseEntity(
        prisonerId = "A1234BC",
        caseUniqueIdentifier = "CASE-1",
        createdBy = "TEST",
        statusId = CourtCaseEntityStatus.ACTIVE,
      )

      val charge = ChargeEntity(
        chargeUuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = LocalDate.of(2020, 1, 1),
        offenceEndDate = null,
        statusId = ChargeEntityStatus.ACTIVE,
        chargeOutcome = null,
        supersedingCharge = null,
        terrorRelated = null,
        foreignPowerRelated = null,
        domesticViolenceRelated = null,
        createdBy = "TEST",
        createdPrison = null,
        legacyData = null,
      )

      val sentence = SentenceEntity(
        sentenceUuid = UUID.randomUUID(),
        statusId = SentenceEntityStatus.ACTIVE,
        createdAt = sentenceCreatedAt,
        createdBy = "TEST",
        createdPrison = null,
        sentenceServeType = "CONCURRENT",
        consecutiveTo = null,
        sentenceType = null,
        supersedingSentence = null,
        charge = charge,
        convictionDate = null,
        legacyData = postedDate?.let {
          SentenceLegacyData(
            postedDate = it,
            bookingId = 123L,
          )
        },
        fineAmount = null,
      )

      charge.sentences = mutableSetOf(sentence)

      val appearance = CourtAppearanceEntity(
        appearanceUuid = UUID.randomUUID(),
        appearanceOutcome = null,
        courtCase = courtCase,
        courtCode = "C1",
        courtCaseReference = "REF1",
        appearanceDate = LocalDate.of(2020, 2, 1),
        statusId = CourtAppearanceEntityStatus.ACTIVE,
        createdBy = "TEST",
        createdPrison = null,
        warrantType = "SENTENCING",
        nextCourtAppearance = null,
        overallConvictionDate = null,
        legacyData = null,
      )

      val appearanceCharge = AppearanceChargeEntity(
        courtAppearanceEntity = appearance,
        chargeEntity = charge,
        createdBy = "TEST",
        createdPrison = null,
      )

      appearance.appearanceCharges.add(appearanceCharge)
      charge.appearanceCharges.add(appearanceCharge)

      courtCase.appearances = setOf(appearance)
      courtCase.latestCourtAppearance = appearance

      return courtCase
    }

    private fun periodLength(
      years: Int? = null,
      months: Int? = null,
      weeks: Int? = null,
      days: Int? = null,
      type: PeriodLengthType = PeriodLengthType.SENTENCE_LENGTH,
    ) = PeriodLength(
      years = years,
      months = months,
      weeks = weeks,
      days = days,
      periodOrder = "years",
      periodLengthType = type,
      legacyData = null,
      periodLengthUuid = UUID.randomUUID(),
    )
  }
}
