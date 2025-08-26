package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ConsecutiveSentenceDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

class SentenceServiceTests {
  private val serviceUserService = mockk<ServiceUserService>(relaxed = true)
  private val periodLengthService = mockk<PeriodLengthService>(relaxed = true)
  private val fixManyChargesToSentenceService = mockk<FixManyChargesToSentenceService>(relaxed = true)
  private val sentenceRepository = mockk<SentenceRepository>(relaxed = true)
  private val sentenceTypeRepository = mockk<SentenceTypeRepository>(relaxed = true)
  private val sentenceHistoryRepository = mockk<SentenceHistoryRepository>(relaxed = true)
  private val recallSentenceRepository = mockk<RecallSentenceRepository>(relaxed = true)

  private val sentenceService = SentenceService(
    sentenceRepository = sentenceRepository,
    periodLengthService = periodLengthService,
    serviceUserService = serviceUserService,
    sentenceTypeRepository = sentenceTypeRepository,
    sentenceHistoryRepository = sentenceHistoryRepository,
    fixManyChargesToSentenceService = fixManyChargesToSentenceService,
    recallSentenceRepository = recallSentenceRepository,
  )

  @Nested
  inner class ExtractChainTests {
    @Test
    fun `should return only the source sentence in chain if nothing is consecutive to source`() {
      val one = getId(1)
      val two = getId(2)
      val three = getId(3)

      val sentenceOne = createConsecutiveSentenceDetails(uuid = one, parent = null)
      val sentenceTwo = createConsecutiveSentenceDetails(uuid = two, parent = one)
      val sentenceThree = createConsecutiveSentenceDetails(uuid = three, parent = null)

      val sentences = listOf(sentenceOne, sentenceTwo, sentenceThree)
      val upstreamChain = sentenceService.getUpstreamChains(sentences, two)

      assertThat(upstreamChain).isEqualTo(listOf(listOf(sentenceTwo)))
    }

    @Test
    fun `should return the correct chain when multiple sentences and multiple chains exist`() {
      val one = getId(1)
      val two = getId(2)
      val three = getId(3)
      val four = getId(4)
      val five = getId(5)
      val six = getId(6)
      val seven = getId(7)

      val sentenceOne = createConsecutiveSentenceDetails(uuid = one, parent = null)
      val sentenceTwo = createConsecutiveSentenceDetails(uuid = two, parent = one)
      val sentenceThree = createConsecutiveSentenceDetails(uuid = three, parent = null)
      val sentenceFour = createConsecutiveSentenceDetails(uuid = four, parent = two)
      val sentenceFive = createConsecutiveSentenceDetails(uuid = five, parent = three)
      val sentenceSix = createConsecutiveSentenceDetails(uuid = six, parent = five)
      val sentenceSeven = createConsecutiveSentenceDetails(uuid = seven, parent = six)

      val sentences =
        listOf(sentenceOne, sentenceTwo, sentenceThree, sentenceFour, sentenceFive, sentenceSix, sentenceSeven)
      val upstreamChain = sentenceService.getUpstreamChains(sentences, two)

      assertThat(upstreamChain).isEqualTo(listOf(listOf(sentenceTwo, sentenceFour)))
    }

    @Test
    fun `should return the correct chain when big chain exists`() {
      val one = getId(1)
      val two = getId(2)
      val three = getId(3)
      val four = getId(4)
      val five = getId(5)
      val six = getId(6)
      val seven = getId(7)

      // Two chains
      // 5 -> 4 -> 2
      // 7 -> 6 -> 3 (wont be returned as source is 2)
      val sentenceOne = createConsecutiveSentenceDetails(uuid = one, parent = null)
      val sentenceTwo = createConsecutiveSentenceDetails(uuid = two, parent = one)
      val sentenceThree = createConsecutiveSentenceDetails(uuid = three, parent = null)
      val sentenceFour = createConsecutiveSentenceDetails(uuid = four, parent = two)
      val sentenceFive = createConsecutiveSentenceDetails(uuid = five, parent = four)
      val sentenceSix = createConsecutiveSentenceDetails(uuid = six, parent = three)
      val sentenceSeven = createConsecutiveSentenceDetails(uuid = seven, parent = six)

      val sentences =
        listOf(sentenceOne, sentenceTwo, sentenceThree, sentenceFour, sentenceFive, sentenceSix, sentenceSeven)
      val upstreamChain = sentenceService.getUpstreamChains(sentences, two)

      assertThat(upstreamChain).isEqualTo(
        listOf(
          listOf(sentenceTwo, sentenceFour, sentenceFive),
        ),
      )
    }

    @Test
    fun `should return the correct chains when source branches into multiple chains`() {
      val one = getId(1)
      val two = getId(2)
      val three = getId(3)
      val four = getId(4)
      val five = getId(5)
      val six = getId(6)
      val seven = getId(7)
      val eight = getId(8)
      val nine = getId(9)

      // two chains here with 2 as the source
      // 9 -> 8 -> 7 -> 3 -> 2
      // 6 -> 5 -> 4 -> 3 -> 2
      val sentenceOne = createConsecutiveSentenceDetails(uuid = one, parent = null)
      val sentenceTwo = createConsecutiveSentenceDetails(uuid = two, parent = one)
      val sentenceThree = createConsecutiveSentenceDetails(uuid = three, parent = two)
      val sentenceFour = createConsecutiveSentenceDetails(uuid = four, parent = three)
      val sentenceFive = createConsecutiveSentenceDetails(uuid = five, parent = four)
      val sentenceSix = createConsecutiveSentenceDetails(uuid = six, parent = five)
      val sentenceSeven = createConsecutiveSentenceDetails(uuid = seven, parent = three)
      val sentenceEight = createConsecutiveSentenceDetails(uuid = eight, parent = seven)
      val sentenceNine = createConsecutiveSentenceDetails(uuid = nine, parent = eight)

      val sentences = listOf(
        sentenceOne,
        sentenceTwo,
        sentenceThree,
        sentenceFour,
        sentenceFive,
        sentenceSix,
        sentenceSeven,
        sentenceEight,
        sentenceNine,
      )
      val upstreamChain = sentenceService.getUpstreamChains(sentences, two)

      assertThat(upstreamChain).isEqualTo(
        listOf(
          listOf(sentenceTwo, sentenceThree, sentenceFour, sentenceFive, sentenceSix),
          listOf(sentenceTwo, sentenceThree, sentenceSeven, sentenceEight, sentenceNine),
        ),
      )
    }
  }

  private fun getId(finalDigit: Int): UUID = UUID.fromString("00000000-0000-0000-0000-00000000000$finalDigit")

  private fun createConsecutiveSentenceDetails(
    uuid: UUID,
    parent: UUID?,
  ): ConsecutiveSentenceDetails = ConsecutiveSentenceDetails(
    uuid,
    parent,
  )
}
