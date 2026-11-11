package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class AnyPrisonerHasActiveCaseWithSentenceOnLatestRecallTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

  @BeforeEach
  fun setUp() {
    adjustmentsApi.stubAllowCreateAdjustments()
    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
  }

  @Test
  fun `should return false when the prisoner has no recalls`() {
    createCourtCaseTwoSentences()

    assertThat(courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isFalse()
  }

  @Test
  fun `should return false when the only recall belongs to another prisoner`() {
    createCourtCaseTwoSentences()
    val (otherSentence, _) = createCourtCaseTwoSentences(prisonerId = "PRI124")
    createRecall(DpsDataCreator.dpsCreateRecall(prisonerId = "PRI124", sentenceIds = listOf(otherSentence.sentenceUuid)))

    assertThat(courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isFalse()
  }

  @Test
  fun `should return true when an active sentence is on the latest recall`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceOne.sentenceUuid)))
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceTwo.sentenceUuid)))

    assertThat(courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isTrue()
  }

  @Test
  fun `should return false when inactive sentence is on the latest recall even if an active sentence is on an older recall`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceOne.sentenceUuid)))
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceTwo.sentenceUuid)))
    setSentenceStatus(sentenceTwo.sentenceUuid, SentenceEntityStatus.INACTIVE)

    assertThat(courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isFalse()
  }

  @Test
  fun `should return false when the court case is not active`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceOne.sentenceUuid)))
    val courtCases = courtCaseRepository.findAllByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    courtCases.forEach { it.statusId = CourtCaseEntityStatus.INACTIVE }
    courtCaseRepository.saveAll(courtCases)

    assertThat(courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isFalse()
  }

  @Test
  fun `should ignore a deleted latest recall and use previous one`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()

    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceOne.sentenceUuid)))
    val latestRecallUuid = createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceTwo.sentenceUuid))).recallUuid

    setSentenceStatus(sentenceTwo.sentenceUuid, SentenceEntityStatus.INACTIVE)
    adjustmentsApi.stubGetRecallAdjustments(DpsDataCreator.DEFAULT_PRISONER_ID, latestRecallUuid.toString(), emptyList())

    deleteRecall(latestRecallUuid)

    assertThat(courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isTrue()
  }

  @Test
  fun `should return true when any of several prisoners is on recall even if another prisoner has a newer recall`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceOne.sentenceUuid)))
    val (otherSentence, _) = createCourtCaseTwoSentences(prisonerId = "PRI124")
    createRecall(DpsDataCreator.dpsCreateRecall(prisonerId = "PRI124", sentenceIds = listOf(otherSentence.sentenceUuid)))
    setSentenceStatus(otherSentence.sentenceUuid, SentenceEntityStatus.INACTIVE)

    assertThat(
      courtCaseRepository.anyPrisonerHasActiveCaseWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID, "PRI124")),
    ).isTrue()
  }

  @Test
  fun `should return list of prisoners on recall skipping prisoners not on recall`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    createRecall(DpsDataCreator.dpsCreateRecall(sentenceIds = listOf(sentenceOne.sentenceUuid)))
    val (otherSentence, _) = createCourtCaseTwoSentences(prisonerId = "PRI124")
    createRecall(DpsDataCreator.dpsCreateRecall(prisonerId = "PRI124", sentenceIds = listOf(otherSentence.sentenceUuid)))
    setSentenceStatus(otherSentence.sentenceUuid, SentenceEntityStatus.INACTIVE)

    assertThat(
      courtCaseRepository.findPrisonerIdsActiveCasesWithSentenceOnLatestRecall(setOf(DpsDataCreator.DEFAULT_PRISONER_ID, "PRI124")),
    ).containsExactly(DpsDataCreator.DEFAULT_PRISONER_ID)
  }

  private fun setSentenceStatus(sentenceUuid: UUID, status: SentenceEntityStatus) {
    val sentences = sentenceRepository.findBySentenceUuid(sentenceUuid)
    sentences.forEach { it.statusId = status }
    sentenceRepository.saveAll(sentences)
  }
}
