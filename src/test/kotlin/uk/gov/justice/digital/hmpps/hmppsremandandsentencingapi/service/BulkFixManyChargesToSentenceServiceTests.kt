package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
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
  private lateinit var dpsDomainEventService: DpsDomainEventService

  @MockK
  private lateinit var courtCaseEntity1: CourtCaseEntity

  @MockK
  private lateinit var courtCaseEntity2: CourtCaseEntity

  @MockK
  private lateinit var courtCaseEntity3: CourtCaseEntity

  @MockK
  private lateinit var courtCaseEntity4: CourtCaseEntity

  @InjectMockKs(overrideValues = true)
  private lateinit var bulkFixManyChargesToSentenceService: BulkFixManyChargesToSentenceService

  @Test
  fun `should call fixCourtCaseSentences multiple times returning the sum of all generated events`() {
    every { courtCaseSarRepository.findIdWithManyChargesDataFixByUpdatedAtDesc(4) } returns setOf(1, 2, 3, 4)
    every { fixManyChargesToSentenceService.fixCourtCasesById(setOf(1, 2, 3, 4), "BATCH_JOB") } returns events(40)
    every { dpsDomainEventService.emitEvents(any()) } just Runs

    // Act
    bulkFixManyChargesToSentenceService.fixCourtCaseSentences(4)

    verify(exactly = 1) { dpsDomainEventService.emitEvents(withArg { assertThat(it).size().isEqualTo(10) }) }
    verify(exactly = 1) { fixManyChargesToSentenceService.fixCourtCasesById(any(), "BATCH_JOB") }
  }

  @Test
  fun `should call fixCourtCaseSentences once returning the generated events for that fix`() {
    every { courtCaseSarRepository.findIdWithManyChargesDataFixByUpdatedAtDesc(1) } returns setOf(
      1,
    )

    every { fixManyChargesToSentenceService.fixCourtCasesById(setOf(1), "BATCH_JOB") } returns events(5)
    every { dpsDomainEventService.emitEvents(any()) } just Runs

    // Act
    bulkFixManyChargesToSentenceService.fixCourtCaseSentences(1)

    verify(exactly = 1) { dpsDomainEventService.emitEvents(withArg { assertThat(it).size().isEqualTo(10) }) }
    verify(exactly = 1) { fixManyChargesToSentenceService.fixCourtCasesById(any(), "BATCH_JOB") }
  }

  @Test
  fun `should do nothing when query returns and empty list`() {
    every { courtCaseSarRepository.findIdWithManyChargesDataFixByUpdatedAtDesc(1) } returns setOf()
    every { fixManyChargesToSentenceService.fixCourtCasesById(setOf(), "BATCH_JOB") } returns mutableSetOf()
    every { dpsDomainEventService.emitEvents(any()) } just Runs

    // Act
    bulkFixManyChargesToSentenceService.fixCourtCaseSentences(1)

    verify(exactly = 1) { dpsDomainEventService.emitEvents(withArg { assertThat(it).size().isEqualTo(0) }) }
    verify(exactly = 1) { fixManyChargesToSentenceService.fixCourtCasesById(any(), "BATCH_JOB") }
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
