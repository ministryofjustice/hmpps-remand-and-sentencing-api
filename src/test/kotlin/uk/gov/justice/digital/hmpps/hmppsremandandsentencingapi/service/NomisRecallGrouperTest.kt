package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallCourtCaseDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecalledSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class NomisRecallGrouperTest {

  @Test
  fun `groups NOMIS recalls with the same recall type and arrest date`() {
    val arrestDate = LocalDate.of(2023, 6, 15)
    val earlier = nomisRecall(
      returnToCustodyDate = arrestDate,
      postedDate = "2023-06-15T10:00:00",
      createdAt = ZonedDateTime.parse("2023-06-16T10:00:00Z"),
      courtCases = listOf(courtCase("CC1")),
    )
    val later = nomisRecall(
      returnToCustodyDate = arrestDate,
      postedDate = "2023-06-16T11:00:00",
      createdAt = ZonedDateTime.parse("2023-06-17T10:00:00Z"),
      courtCases = listOf(courtCase("CC2")),
    )

    val result = NomisRecallGrouper.group(listOf(earlier, later))

    assertThat(result).hasSize(1)
    assertThat(result[0].recallUuid).isEqualTo(earlier.recallUuid)
    assertThat(result[0].courtCases.map { it.courtCaseReference }).containsExactlyInAnyOrder("CC1", "CC2")
  }

  @Test
  fun `groups NOMIS recalls by posted date day when there is no arrest date`() {
    val morning = nomisRecall(
      returnToCustodyDate = null,
      postedDate = "2023-06-15T10:00:00",
      createdAt = ZonedDateTime.parse("2023-06-16T10:00:00Z"),
      courtCases = listOf(courtCase("CC1")),
    )
    val evening = nomisRecall(
      returnToCustodyDate = null,
      postedDate = "2023-06-15T23:59:59",
      createdAt = ZonedDateTime.parse("2023-06-17T10:00:00Z"),
      courtCases = listOf(courtCase("CC2")),
    )

    assertThat(NomisRecallGrouper.group(listOf(morning, evening))).hasSize(1)
  }

  @Test
  fun `does not group NOMIS recalls with different recall types`() {
    val ftr = nomisRecall(
      recallType = RecallType.FTR_56,
      returnToCustodyDate = LocalDate.of(2023, 6, 15),
    )
    val lr = nomisRecall(
      recallType = RecallType.LR,
      returnToCustodyDate = LocalDate.of(2023, 6, 15),
    )

    assertThat(NomisRecallGrouper.group(listOf(ftr, lr))).hasSize(2)
  }

  @Test
  fun `does not group NOMIS recalls with different arrest dates`() {
    val first = nomisRecall(returnToCustodyDate = LocalDate.of(2023, 6, 15))
    val second = nomisRecall(returnToCustodyDate = LocalDate.of(2023, 6, 16))

    assertThat(NomisRecallGrouper.group(listOf(first, second))).hasSize(2)
  }

  @Test
  fun `does not duplicate sentences that share a uuid when merging`() {
    val sentenceUuid = UUID.randomUUID()
    val courtCaseUuid = UUID.randomUUID().toString()
    val first = nomisRecall(
      returnToCustodyDate = LocalDate.of(2023, 6, 15),
      courtCases = listOf(courtCase("CC1", courtCaseUuid, sentenceUuid)),
    )
    val second = nomisRecall(
      returnToCustodyDate = LocalDate.of(2023, 6, 15),
      courtCases = listOf(courtCase("CC1", courtCaseUuid, sentenceUuid)),
    )

    val result = NomisRecallGrouper.group(listOf(first, second))

    assertThat(result).hasSize(1)
    assertThat(result[0].courtCases.single().sentences).hasSize(1)
  }

  @Test
  fun `does not group DPS recalls`() {
    val first = dpsRecall(revocationDate = LocalDate.of(2024, 1, 1))
    val second = dpsRecall(revocationDate = LocalDate.of(2024, 1, 1))

    assertThat(NomisRecallGrouper.group(listOf(first, second))).hasSize(2)
  }

  private fun nomisRecall(
    recallUuid: UUID = UUID.randomUUID(),
    recallType: RecallType = RecallType.FTR_28,
    returnToCustodyDate: LocalDate? = LocalDate.of(2023, 1, 1),
    postedDate: String? = "2023-01-01T00:00:00",
    createdAt: ZonedDateTime = ZonedDateTime.parse("2023-01-02T10:00:00Z"),
    courtCases: List<RecallCourtCaseDetails> = listOf(courtCase("CC1")),
  ) = Recall(
    recallUuid = recallUuid,
    prisonerId = "A1234BC",
    revocationDate = null,
    returnToCustodyDate = returnToCustodyDate,
    inPrisonOnRevocationDate = null,
    recallType = recallType,
    createdAt = createdAt,
    createdByUsername = "USER1",
    createdByPrison = null,
    source = EventSource.NOMIS,
    postedDate = postedDate,
    courtCases = courtCases,
  )

  private fun dpsRecall(revocationDate: LocalDate) = Recall(
    recallUuid = UUID.randomUUID(),
    prisonerId = "A1234BC",
    revocationDate = revocationDate,
    returnToCustodyDate = null,
    inPrisonOnRevocationDate = null,
    recallType = RecallType.LR,
    createdAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"),
    createdByUsername = "USER1",
    createdByPrison = "PRISON1",
    source = EventSource.DPS,
  )

  private fun courtCase(
    reference: String,
    courtCaseUuid: String = UUID.randomUUID().toString(),
    sentenceUuid: UUID = UUID.randomUUID(),
  ) = RecallCourtCaseDetails(
    courtCaseReference = reference,
    courtCaseUuid = courtCaseUuid,
    courtCode = "COURT1",
    sentencingAppearanceDate = LocalDate.of(2020, 1, 1),
    bookingId = 1L,
    sentences = listOf(
      RecalledSentence(
        sentenceUuid = sentenceUuid,
        offenceCode = "OF1",
        offenceStartDate = null,
        offenceEndDate = null,
        sentenceDate = null,
        lineNumber = null,
        countNumber = null,
        periodLengths = emptyList(),
        sentenceServeType = "CONCURRENT",
        sentenceTypeDescription = null,
      ),
    ),
  )
}
