package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository

@ExtendWith(MockKExtension::class)
class BulkFixManyChargesToSentenceServiceTests {

  @MockK
  private lateinit var courtCaseSarRepository: CourtCaseRepository

  @MockK
  private lateinit var fixManyChargesToSentenceService: FixManyChargesToSentenceService

  @MockK
  private lateinit var courtCaseEntity1: CourtCaseEntity

  @MockK
  private lateinit var courtCaseEntity2: CourtCaseEntity

  @MockK
  private lateinit var courtCaseEntity3: CourtCaseEntity

  @MockK
  private lateinit var courtCaseEntity4: CourtCaseEntity

  @InjectMockKs(overrideValues = true)
  private lateinit var sut: BulkFixManyChargesToSentenceService

  @Test
  fun `should call fixCourtCaseSentences multiple times returning the sum of all generated events`() {
    every { courtCaseSarRepository.findCaseUniqueIdentifierWithManyChargesDataFixByUpdatedAtDesc(4) } returns listOf(
      "70D8677F-3E37-487D-ADA7-EEFE3182438B",
      "B9C71F09-A5C0-4355-B961-BFC229EE7104",
      "28DA01FA-EEC5-4541-890C-F0A0FDD11772",
      "001AE716-4F8E-4C04-B855-2DCCCF5EFA24",
    )

    every { courtCaseSarRepository.findSentencedCourtCase("70D8677F-3E37-487D-ADA7-EEFE3182438B") } returns courtCaseEntity1
    every { courtCaseSarRepository.findSentencedCourtCase("B9C71F09-A5C0-4355-B961-BFC229EE7104") } returns courtCaseEntity2
    every { courtCaseSarRepository.findSentencedCourtCase("28DA01FA-EEC5-4541-890C-F0A0FDD11772") } returns courtCaseEntity3
    every { courtCaseSarRepository.findSentencedCourtCase("001AE716-4F8E-4C04-B855-2DCCCF5EFA24") } returns courtCaseEntity4
    every { fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity1, "BATCH_JOB") } returns events(5)
    every { fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity2, "BATCH_JOB") } returns events(3)
    every { fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity3, "BATCH_JOB") } returns events(66)
    every { fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity4, "BATCH_JOB") } returns events(1)

    val events = sut.fixCourtCaseSentences(4)

    assertThat(events).size().isEqualTo(40)
    verify(exactly = 4) { fixManyChargesToSentenceService.fixCourtCaseSentences(any(), "BATCH_JOB") }
  }

  @Test
  fun `should call fixCourtCaseSentences once returning the generated events for that fix`() {
    every { courtCaseSarRepository.findCaseUniqueIdentifierWithManyChargesDataFixByUpdatedAtDesc(1) } returns listOf(
      "70D8677F-3E37-487D-ADA7-EEFE3182438B",
    )

    every { courtCaseSarRepository.findSentencedCourtCase("70D8677F-3E37-487D-ADA7-EEFE3182438B") } returns courtCaseEntity1
    every { fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity1, "BATCH_JOB") } returns events(5)

    val events = sut.fixCourtCaseSentences(1)

    assertThat(events).size().isEqualTo(10)
    verify(exactly = 1) { fixManyChargesToSentenceService.fixCourtCaseSentences(any(), "BATCH_JOB") }
  }

  @Test
  fun `should do nothing when query returns and empty list`() {
    every { courtCaseSarRepository.findCaseUniqueIdentifierWithManyChargesDataFixByUpdatedAtDesc(1) } returns listOf()

    val events = sut.fixCourtCaseSentences(1)

    assertThat(events).size().isEqualTo(0)
    verify(exactly = 0) { fixManyChargesToSentenceService.fixCourtCaseSentences(any(), "BATCH_JOB") }
  }

  fun events(prisonerId: Int): MutableSet<EventMetadata> = mutableSetOf(
    EventMetadata(prisonerId = "A1234AA$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "B2345BB$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "C3456CC$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "D4567DD$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "E5678EE$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "F6789FF$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "G7890GG$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "H8901HH$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "I9012II$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
    EventMetadata(prisonerId = "J0123JJ$prisonerId", null, null, null, null, null, eventType = EventType.SENTENCE_UPDATED),
  )
}
