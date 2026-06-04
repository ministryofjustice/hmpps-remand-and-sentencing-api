package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

class PostCleanupManyChargesToSentenceTests : IntegrationTestBase() {

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `should cleanup many charges of a single sentence`() {
    arrange(totalChargesToSentence = 4, totalCourtCases = 30)
    val initialOverallSentenceCount = getSentenceCount();
    val initialDuplicateSentences = getDuplicateSentenceCount();
    assertThat(initialDuplicateSentences).isEqualTo(30)
    assertThat(initialOverallSentenceCount).isEqualTo(120) // 30 * 4

    // Act
    webTestClient
      .post()
      .uri("/court-case-admin/cleanup-many-charges-to-sentence")
      .headers { it.contentType = MediaType.APPLICATION_JSON }
      .exchange()
      .expectStatus().isOk

    val totalCorrectedSentences = initialDuplicateSentences?.minus(getDuplicateSentenceCount() ?: 0)
    assertThat(totalCorrectedSentences).isEqualTo(30)
    val overallTotalSentenceCount = getSentenceCount();
    assertThat(overallTotalSentenceCount).isEqualTo(120)

    val totalMessage = getMessages(expectedNumberOfMessages = 210).count { p -> p.eventType == "sentence.fix-single-charge.inserted" }
    assertThat(totalMessage).isEqualTo(90) // total charges - 1 * court cases fixed
  }

  @Test
  fun `should not affect single charges to a single sentence`() {
    arrange(totalChargesToSentence = 1, totalCourtCases = 30)
    val initialOverallSentenceCount = getSentenceCount();
    val initialDuplicateSentences = getDuplicateSentenceCount();
    assertThat(initialDuplicateSentences).isEqualTo(0)
    assertThat(initialOverallSentenceCount).isEqualTo(30) // 30 * 1

    // Act
    webTestClient
      .post()
      .uri("/court-case-admin/cleanup-many-charges-to-sentence")
      .headers { it.contentType = MediaType.APPLICATION_JSON }
      .exchange()
      .expectStatus().isOk

    val totalCorrectedSentences = initialDuplicateSentences?.minus(getDuplicateSentenceCount() ?: 0)
    assertThat(totalCorrectedSentences).isEqualTo(0)
    val overallTotalSentenceCount = getSentenceCount()
    assertThat(overallTotalSentenceCount).isEqualTo(30)

    val totalMessage = getMessages(expectedNumberOfMessages = 0).count()
    assertThat(totalMessage).isEqualTo(0)
  }

  @Test
  fun `should run without any sentences in the db`() {
    val initialOverallSentenceCount = getSentenceCount();
    assertThat(initialOverallSentenceCount).isEqualTo(0)

    // Act
    webTestClient
      .post()
      .uri("/court-case-admin/cleanup-many-charges-to-sentence")
      .headers { it.contentType = MediaType.APPLICATION_JSON }
      .exchange()
      .expectStatus().isOk

    val overallTotalSentenceCount = getSentenceCount()
    assertThat(overallTotalSentenceCount).isEqualTo(0)

    val totalMessage = getMessages(expectedNumberOfMessages = 0).count()
    assertThat(totalMessage).isEqualTo(0)
  }

  private fun getSentenceCount(): Long? = jdbcTemplate.queryForObject<Long>(
    """SELECT count(*)
             FROM sentence             
    """.trimIndent(),
  )

  private fun getDuplicateSentenceCount(): Long? = jdbcTemplate.queryForObject<Long>(
    """SELECT COUNT(*)
             FROM (SELECT sentence_uuid
                  FROM sentence
                  GROUP BY sentence_uuid
                  HAVING COUNT(*) > 1) duplicates
    """.trimIndent(),
  )

  private fun arrange(totalChargesToSentence: Long = 50, totalCourtCases: Long = 1) {
    (1L..totalCourtCases).forEach { s ->
      val sentence = DataCreator.migrationCreateSentence()
      val charges = mutableListOf<MigrationCreateCharge>()
      (1L..totalChargesToSentence).forEach { i ->
        charges.add(DataCreator.migrationCreateCharge(chargeNOMISId = i, sentence = sentence))
      }
      val appearance = DataCreator.migrationCreateCourtAppearance(charges = charges)
      val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
      val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))
      webTestClient
        .post()
        .uri("/legacy/court-case/migration")
        .bodyValue(courtCases)
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
          it.contentType = MediaType.APPLICATION_JSON
        }
        .exchange()
        .expectStatus()
        .isCreated
        .returnResult<MigrationCreateCourtCasesResponse>()
        .responseBody.blockFirst()!!
    }
  }

}