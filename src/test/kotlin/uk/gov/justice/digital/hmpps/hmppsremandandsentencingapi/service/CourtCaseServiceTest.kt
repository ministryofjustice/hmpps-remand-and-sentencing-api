package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class CourtCaseServiceTest {

  private val courtCaseRepository = mockk<uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository>()
  private val courtAppearanceService = mockk<CourtAppearanceService>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val fixManyChargesToSentenceService = mockk<FixManyChargesToSentenceService>()

  private val service = CourtCaseService(
    courtCaseRepository,
    courtAppearanceService,
    serviceUserService,
    fixManyChargesToSentenceService,
  )

  @Nested
  inner class MergeAndSortRecallableCourtCases {
    @Test
    fun `dedupes duplicate sentences, merges court cases, and keeps non duplicates`() {
      val dupKeyOffence = "OFF1"
      val dupStart = LocalDate.of(2020, 1, 1)
      val dupSentenceDate = LocalDate.of(2020, 2, 1)

      val olderDup = sentence(
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

      val uniqueA = sentence(UUID.randomUUID(), "A1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0))
      val uniqueB = sentence(UUID.randomUUID(), "B1", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0))

      val case1 = courtCase(
        uuid = "CASE-1",
        courtCode = "C1",
        appearanceDate = LocalDate.of(2024, 1, 10),
        sentences = listOf(olderDup, uniqueA),
      )
      val case2 = courtCase(
        uuid = "CASE-2",
        courtCode = "C1",
        appearanceDate = LocalDate.of(2024, 1, 5),
        sentences = listOf(newerDup, uniqueB),
      )

      val result = service.mergeAndSortCourtCases(listOf(case1, case2))

      assertThat(result).hasSize(1)

      val merged = result.single()
      assertThat(merged.courtCode).isEqualTo("C1")
      assertThat(merged.courtCaseUuid).isEqualTo("CASE-1")
      assertThat(merged.sentences.map { it.offenceCode }).containsExactlyInAnyOrder(dupKeyOffence, "A1", "B1")
      assertThat(merged.sentences.filter { it.offenceCode == dupKeyOffence }).hasSize(1)
      assertThat(merged.sentences.single { it.offenceCode == dupKeyOffence }.createdAt)
        .isEqualTo(LocalDateTime.of(2020, 2, 1, 10, 0))
    }

    @Test
    fun `drops court cases that become empty after dedupe`() {
      val dupSentenceDate = LocalDate.of(2020, 2, 1)

      val olderDup = sentence(
        uuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = LocalDate.of(2020, 1, 1),
        sentenceDate = dupSentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 1, 10, 0),
      )
      val newerDupOnly = sentence(
        uuid = UUID.randomUUID(),
        offenceCode = "OFF1",
        offenceStartDate = LocalDate.of(2020, 1, 1),
        sentenceDate = dupSentenceDate,
        createdAt = LocalDateTime.of(2020, 2, 2, 10, 0),
      )

      val winnerCase = courtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(olderDup))
      val loserOnlyCase = courtCase("CASE-2", "C1", LocalDate.of(2024, 1, 5), listOf(newerDupOnly))

      val result = service.mergeAndSortCourtCases(listOf(winnerCase, loserOnlyCase))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-1")
    }

    @Test
    fun `sorts merged cases by appearanceDate desc`() {
      val s1 = sentence(UUID.randomUUID(), "X1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))
      val s2 = sentence(UUID.randomUUID(), "Y1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 10, 0))

      val newer = courtCase("CASE-NEW", "C1", LocalDate.of(2024, 2, 1), listOf(s1))
      val older = courtCase("CASE-OLD", "C2", LocalDate.of(2024, 1, 1), listOf(s2))

      val result = service.mergeAndSortCourtCases(listOf(older, newer))

      assertThat(result.map { it.courtCaseUuid }).containsExactly("CASE-NEW", "CASE-OLD")
    }

    @Test
    fun `merges 4 court cases with shared duplicate into earliest duplicate sentence court case`() {
      val earliestDup1 = sentence(UUID.randomUUID(), "OFF-DUPLICATE", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))
      val dup2 = earliestDup1.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 2, 10, 0))
      val dup3 = earliestDup1.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 3, 10, 0))
      val dup4 = earliestDup1.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 4, 10, 0))

      val unique1 = sentence(UUID.randomUUID(), "U1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0))
      val unique2 = sentence(UUID.randomUUID(), "U2", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0))
      val unique3 = sentence(UUID.randomUUID(), "U3", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 2, 1), LocalDateTime.of(2017, 2, 1, 9, 0))
      val unique4 = sentence(UUID.randomUUID(), "U4", LocalDate.of(2016, 1, 1), LocalDate.of(2016, 2, 1), LocalDateTime.of(2016, 2, 1, 9, 0))

      val case1 = courtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(earliestDup1, unique1))
      val case2 = courtCase("CASE-2", "C1", LocalDate.of(2024, 1, 9), listOf(dup2, unique2))
      val case3 = courtCase("CASE-3", "C1", LocalDate.of(2024, 1, 8), listOf(dup3, unique3))
      val case4 = courtCase("CASE-4", "C1", LocalDate.of(2024, 1, 7), listOf(dup4, unique4))

      val result = service.mergeAndSortCourtCases(listOf(case4, case2, case3, case1))

      assertThat(result).hasSize(1)
      val merged = result.single()

      // representative is the case containing the earliest duplicate
      assertThat(merged.courtCaseUuid).isEqualTo("CASE-1")

      // keeps earliest dup + all uniques
      assertThat(merged.sentences.map { it.offenceCode })
        .containsExactlyInAnyOrder("OFF-DUPLICATE", "U1", "U2", "U3", "U4")

      assertThat(merged.sentences.filter { it.offenceCode == "OFF-DUPLICATE" }).hasSize(1)
      assertThat(merged.sentences.single { it.offenceCode == "OFF-DUPLICATE" }.createdAt)
        .isEqualTo(LocalDateTime.of(2020, 2, 1, 10, 0))
    }

    @Test
    fun `returns input unchanged when there are no duplicates across 4 court cases`() {
      val c1 = courtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(sentence(UUID.randomUUID(), "O1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))),
      )
      val c2 = courtCase(
        "CASE-2",
        "C2",
        LocalDate.of(2024, 1, 9),
        listOf(sentence(UUID.randomUUID(), "O2", LocalDate.of(2020, 1, 2), LocalDate.of(2020, 2, 2), LocalDateTime.of(2020, 2, 2, 10, 0))),
      )
      val c3 = courtCase(
        "CASE-3",
        "C3",
        LocalDate.of(2024, 1, 8),
        listOf(sentence(UUID.randomUUID(), "O3", LocalDate.of(2020, 1, 3), LocalDate.of(2020, 2, 3), LocalDateTime.of(2020, 2, 3, 10, 0))),
      )
      val c4 = courtCase(
        "CASE-4",
        "C4",
        LocalDate.of(2024, 1, 7),
        listOf(sentence(UUID.randomUUID(), "O4", LocalDate.of(2020, 1, 4), LocalDate.of(2020, 2, 4), LocalDateTime.of(2020, 2, 4, 10, 0))),
      )

      val result = service.mergeAndSortCourtCases(listOf(c3, c1, c4, c2))
      assertThat(result).isEqualTo(listOf(c1, c2, c3, c4))
    }

    @Test
    fun `merges duplicate cases and leaves non duplicate cases unchanged`() {
      val dupStart = LocalDate.of(2020, 1, 1)
      val dupSentenceDate = LocalDate.of(2020, 2, 1)

      val olderDup = sentence(
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

      val case1 = courtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(
          olderDup,
          sentence(UUID.randomUUID(), "U1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0)),
        ),
      )
      val case2 = courtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(
          newerDup,
          sentence(UUID.randomUUID(), "U2", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0)),
          sentence(UUID.randomUUID(), "U3", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 2, 1), LocalDateTime.of(2017, 2, 1, 9, 0)),
        ),
      )
      val case3 = courtCase(
        "CASE-3",
        "C3",
        LocalDate.of(2024, 1, 8),
        listOf(
          sentence(UUID.randomUUID(), "O3", LocalDate.of(2020, 1, 3), LocalDate.of(2020, 2, 3), LocalDateTime.of(2020, 2, 3, 10, 0)),
        ),
      )
      val case4 = courtCase(
        "CASE-4",
        "C4",
        LocalDate.of(2024, 1, 7),
        listOf(
          sentence(UUID.randomUUID(), "O4", LocalDate.of(2020, 1, 4), LocalDate.of(2020, 2, 4), LocalDateTime.of(2020, 2, 4, 10, 0)),
        ),
      )

      val result = service.mergeAndSortCourtCases(listOf(case4, case2, case3, case1))

      assertThat(result.map { it.courtCaseUuid }).containsExactly("CASE-1", "CASE-3", "CASE-4")

      val resultMergedCase1 = result[0]
      assertThat(resultMergedCase1.courtCaseUuid).isEqualTo("CASE-1")
      assertThat(resultMergedCase1.sentences.map { it.offenceCode }).containsExactlyInAnyOrder("OFF-DUP", "U1", "U2", "U3")

      val resultCase3 = result[1]
      assertThat(resultCase3.courtCaseUuid).isEqualTo("CASE-3")
      assertThat(resultCase3.sentences.map { it.offenceCode }).containsExactly("O3")

      val resultCase4 = result[2]
      assertThat(resultCase4.courtCaseUuid).isEqualTo("CASE-4")
      assertThat(resultCase4.sentences.map { it.offenceCode }).containsExactly("O4")
    }

    @Test
    fun `does not treat duplicate key within the same court case as a cross case duplicate`() {
      val dup1 = sentence(
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

      val case1 = courtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(dup1, dup2),
      )
      val case2 = courtCase(
        "CASE-2",
        "C2",
        LocalDate.of(2024, 1, 9),
        listOf(sentence(UUID.randomUUID(), "O2", LocalDate.of(2020, 1, 2), LocalDate.of(2020, 2, 2), LocalDateTime.of(2020, 2, 2, 9, 0))),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).isEqualTo(listOf(case1, case2))
    }

    @Test
    fun `does not merge when duplicate sentence fields match but court code differs`() {
      val sentenceDate = LocalDate.of(2020, 2, 1)
      val startDate = LocalDate.of(2020, 1, 1)

      val case1 = courtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(sentence(UUID.randomUUID(), "OFF1", startDate, sentenceDate, LocalDateTime.of(2020, 2, 1, 10, 0))),
      )
      val case2 = courtCase(
        "CASE-2",
        "C2",
        LocalDate.of(2024, 1, 9),
        listOf(sentence(UUID.randomUUID(), "OFF1", startDate, sentenceDate, LocalDateTime.of(2020, 2, 2, 10, 0))),
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

      val case1 = courtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(
          sentence(UUID.randomUUID(), "A", keyAStart, keyADate, LocalDateTime.of(2020, 2, 1, 10, 0)),
          sentence(UUID.randomUUID(), "U1", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), LocalDateTime.of(2019, 2, 1, 9, 0)),
        ),
      )
      val case2 = courtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(
          sentence(UUID.randomUUID(), "A", keyAStart, keyADate, LocalDateTime.of(2020, 2, 2, 10, 0)),
          sentence(UUID.randomUUID(), "B", keyBStart, keyBDate, LocalDateTime.of(2020, 4, 1, 10, 0)),
          sentence(UUID.randomUUID(), "U2", LocalDate.of(2018, 1, 1), LocalDate.of(2018, 2, 1), LocalDateTime.of(2018, 2, 1, 9, 0)),
        ),
      )
      val case3 = courtCase(
        "CASE-3",
        "C1",
        LocalDate.of(2024, 1, 8),
        listOf(
          sentence(UUID.randomUUID(), "B", keyBStart, keyBDate, LocalDateTime.of(2020, 4, 2, 10, 0)),
          sentence(UUID.randomUUID(), "U3", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 2, 1), LocalDateTime.of(2017, 2, 1, 9, 0)),
        ),
      )

      val result = service.mergeAndSortCourtCases(listOf(case3, case2, case1))

      assertThat(result).hasSize(1)
      val merged = result.single()
      assertThat(merged.courtCaseUuid).isEqualTo("CASE-1")
      assertThat(merged.sentences.map { it.offenceCode })
        .containsExactlyInAnyOrder("A", "B", "U1", "U2", "U3")
    }

    @Test
    fun `uses case uuid as tiebreaker when duplicate winners have the same created at`() {
      val createdAt = LocalDateTime.of(2020, 2, 1, 10, 0)

      val case1 = courtCase(
        "CASE-1",
        "C1",
        LocalDate.of(2024, 1, 10),
        listOf(sentence(UUID.randomUUID(), "OFF1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), createdAt)),
      )
      val case2 = courtCase(
        "CASE-2",
        "C1",
        LocalDate.of(2024, 1, 9),
        listOf(sentence(UUID.randomUUID(), "OFF1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), createdAt)),
      )

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-1")
    }
  }

  @Nested
  inner class GetRecallableCourtCasesMergeDuplicateCourtCasesFlag {

    @Test
    fun `returns normal sorted cases when merge duplicate court cases is false`() {
      val case1 = courtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(sentence(UUID.randomUUID(), "O1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))))
      val case2 = courtCase("CASE-2", "C1", LocalDate.of(2024, 1, 9), listOf(sentence(UUID.randomUUID(), "O2", LocalDate.of(2020, 1, 2), LocalDate.of(2020, 2, 2), LocalDateTime.of(2020, 2, 2, 10, 0))))

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).isEqualTo(listOf(case1, case2))
    }

    @Test
    fun `returns merged cases when merge duplicate court cases is true`() {
      val olderDup = sentence(UUID.randomUUID(), "OFF1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), LocalDateTime.of(2020, 2, 1, 10, 0))
      val newerDup = olderDup.copy(sentenceUuid = UUID.randomUUID(), createdAt = LocalDateTime.of(2020, 2, 2, 10, 0))

      val case1 = courtCase("CASE-1", "C1", LocalDate.of(2024, 1, 10), listOf(olderDup))
      val case2 = courtCase("CASE-2", "C1", LocalDate.of(2024, 1, 9), listOf(newerDup))

      val result = service.mergeAndSortCourtCases(listOf(case2, case1))

      assertThat(result).hasSize(1)
      assertThat(result.single().courtCaseUuid).isEqualTo("CASE-1")
    }
  }

  @Nested
  inner class GetRecallableCourtCasesCreatedAtMapping {

    @Test
    fun `uses posted date when it is earlier than created at`() {
      val createdAt = ZonedDateTime.parse("2020-02-07T10:00:00Z")
      val postedDate = "2020-02-06T12:48:05.692411"

      every { courtCaseRepository.findSentencedCourtCasesByPrisonerId("A1234BC") } returns listOf(mockCourtCaseWithSentence(createdAt, postedDate))
      every { fixManyChargesToSentenceService.fixCourtCaseSentences(any()) } returns mutableSetOf()

      val result = service.getRecallableCourtCases("A1234BC")

      assertThat(result.record.cases.single().sentences.single().createdAt).isEqualTo(LocalDateTime.parse("2020-02-06T12:48:05.692411"))
    }

    @Test
    fun `uses created at when it is earlier than posted date`() {
      val createdAt = ZonedDateTime.parse("2020-02-05T10:00:00Z")
      val postedDate = "2020-02-06T12:48:05.692411"

      every { courtCaseRepository.findSentencedCourtCasesByPrisonerId("A1234BC") } returns listOf(mockCourtCaseWithSentence(createdAt, postedDate))
      every { fixManyChargesToSentenceService.fixCourtCaseSentences(any()) } returns mutableSetOf()

      val result = service.getRecallableCourtCases("A1234BC")

      assertThat(result.record.cases.single().sentences.single().createdAt).isEqualTo(LocalDateTime.parse("2020-02-05T10:00:00"))
    }

    @Test
    fun `uses created at when posted date is absent`() {
      val createdAt = ZonedDateTime.parse("2020-02-05T10:00:00Z")

      every { courtCaseRepository.findSentencedCourtCasesByPrisonerId("A1234BC") } returns listOf(mockCourtCaseWithSentence(createdAt, null))
      every { fixManyChargesToSentenceService.fixCourtCaseSentences(any()) } returns mutableSetOf()

      val result = service.getRecallableCourtCases("A1234BC")

      assertThat(result.record.cases.single().sentences.single().createdAt).isEqualTo(LocalDateTime.parse("2020-02-05T10:00:00"))
    }
  }

  private fun courtCase(
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

  private fun sentence(
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

  private fun mockCourtCaseWithSentence(
    sentenceCreatedAt: ZonedDateTime,
    postedDate: String?,
  ): CourtCaseEntity {
    val sentence = mockk<SentenceEntity>()
    val charge = mockk<ChargeEntity>()
    val appearanceCharge = mockk<AppearanceChargeEntity>()
    val appearance = mockk<CourtAppearanceEntity>()
    val courtCase = mockk<CourtCaseEntity>()

    every { sentence.id } returns 1
    every { sentence.sentenceUuid } returns UUID.randomUUID()
    every { sentence.charge } returns charge
    every { sentence.createdAt } returns sentenceCreatedAt
    every { sentence.legacyData } returns postedDate?.let {
      SentenceLegacyData(
        postedDate = it,
        bookingId = 123L,
      )
    }
    every { sentence.countNumber } returns null
    every { sentence.sentenceServeType } returns "CONCURRENT"
    every { sentence.periodLengths } returns mutableSetOf()
    every { sentence.convictionDate } returns null
    every { sentence.fineAmount } returns null
    every { sentence.sentenceType } returns null
    every { sentence.consecutiveTo } returns null

    every { charge.statusId } returns ChargeEntityStatus.ACTIVE
    every { charge.getLiveSentence() } returns sentence
    every { charge.offenceCode } returns "OFF1"
    every { charge.offenceStartDate } returns LocalDate.of(2020, 1, 1)
    every { charge.offenceEndDate } returns null
    every { charge.chargeOutcome } returns null
    every { charge.legacyData } returns null

    every { appearanceCharge.charge } returns charge

    every { appearance.statusId } returns CourtAppearanceEntityStatus.ACTIVE
    every { appearance.warrantType } returns "SENTENCING"
    every { appearance.appearanceDate } returns LocalDate.of(2020, 2, 1)
    every { appearance.appearanceCharges } returns mutableSetOf(appearanceCharge)
    every { appearance.courtCode } returns "C1"
    every { appearance.courtCaseReference } returns "REF1"

    every { courtCase.caseUniqueIdentifier } returns "CASE-1"
    every { courtCase.latestCourtAppearance } returns appearance
    every { courtCase.appearances } returns mutableSetOf(appearance)
    every { courtCase.statusId } returns CourtCaseEntityStatus.ACTIVE

    return courtCase
  }
}
