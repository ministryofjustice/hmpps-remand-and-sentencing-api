package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.batch

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException
import org.springframework.batch.test.JobOperatorTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

@SpringBatchTest
@ActiveProfiles("batch", "test", "test-batch")
class ManyChargesToSentenceFixTests : IntegrationTestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var jobOperatorTestUtils: JobOperatorTestUtils

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var manyChargesToSentenceFixTests: Job

  @AfterEach
  fun cleanup() {
    jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT")
    jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT")
    jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION")
    jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_PARAMS")
    jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION")
    jdbcTemplate.execute("DELETE FROM BATCH_JOB_INSTANCE")
  }

  @Test
  fun `should fix the latest 5 court cases`() {
    arrange(4, 30)

    val initialDuplicateSentences = getDuplicateSentenceCount()
    assertThat(initialDuplicateSentences).isEqualTo(30)

    runJob("2026-01-02")

    val totalCorrectedSentences = initialDuplicateSentences?.minus(getDuplicateSentenceCount() ?: 0)
    assertThat(totalCorrectedSentences).isEqualTo(5)
    val totalRemainingSentencesToFix = getDuplicateSentenceCount()
    assertThat(totalRemainingSentencesToFix).isEqualTo(25)
  }

  @Test
  fun `should fix the latest 10 court cases, run on successive days`() {
    arrange(4, 30)
    val initialDuplicateSentences = getDuplicateSentenceCount()
    assertThat(initialDuplicateSentences).isEqualTo(30)

    runJob("2026-01-02")
    runJob("2026-01-03")

    val totalCorrectedSentences = initialDuplicateSentences?.minus(getDuplicateSentenceCount() ?: 0)
    assertThat(totalCorrectedSentences).isEqualTo(10)
    val totalRemainingSentencesToFix = getDuplicateSentenceCount()
    assertThat(totalRemainingSentencesToFix).isEqualTo(20)
  }

  @Test
  fun `should fix all court cases, run on 6 successive days`() {
    arrange(4, 30)
    val initialDuplicateSentences = getDuplicateSentenceCount()
    assertThat(initialDuplicateSentences).isEqualTo(30)

    runJob("2026-01-02")
    runJob("2026-01-03")
    runJob("2026-01-04")
    runJob("2026-01-05")
    runJob("2026-01-06")
    runJob("2026-01-07")

    val totalCorrectedSentences = initialDuplicateSentences?.minus(getDuplicateSentenceCount() ?: 0)
    assertThat(totalCorrectedSentences).isEqualTo(30)
    val totalRemainingSentencesToFix = getDuplicateSentenceCount()
    assertThat(totalRemainingSentencesToFix).isEqualTo(0)
  }

  @Test
  fun `should fix all court cases, run on 7 successive days`() {
    arrange(4, 30)
    val initialDuplicateSentences = getDuplicateSentenceCount()
    assertThat(initialDuplicateSentences).isEqualTo(30)

    runJob("2026-01-02")
    runJob("2026-01-03")
    runJob("2026-01-04")
    runJob("2026-01-05")
    runJob("2026-01-06")
    runJob("2026-01-07")
    runJob("2026-01-08") // This job has no work to do, but still succeeds

    val totalCorrectedSentences = initialDuplicateSentences?.minus(getDuplicateSentenceCount() ?: 0)
    assertThat(totalCorrectedSentences).isEqualTo(30)
    val totalRemainingSentencesToFix = getDuplicateSentenceCount()
    assertThat(totalRemainingSentencesToFix).isEqualTo(0)
  }

  @Test
  fun `should fail when first job succeeds and second job keeps params the same`() {
    arrange(4, 30)

    runJob("2026-01-02")
    assertThatThrownBy {
      runJob("2026-01-02")
    }.isInstanceOf(JobInstanceAlreadyCompleteException::class.java)
      .hasMessage("A job instance already exists and is complete for identifying parameters={JobParameter{name='run.date', value=2026-01-02, type=class java.lang.String, identifying=true}}.  If you want to run this job again, change the parameters.")
  }

  private fun runJob(runDate: String) {
    var params = JobParametersBuilder()
      .addString("run.date", runDate)
      .toJobParameters()
    jobOperatorTestUtils.setJob(manyChargesToSentenceFixTests)
    var result = jobOperatorTestUtils.startJob(params)
    assertThat(result.status).isEqualTo(BatchStatus.COMPLETED)
  }

  private fun getDuplicateSentenceCount(): Long? = jdbcTemplate.queryForObject<Long>(
    """SELECT COUNT(*)
             FROM (SELECT sentence_uuid
                  FROM sentence
                  GROUP BY sentence_uuid
                  HAVING COUNT(*) > 1) duplicates
    """.trimIndent(),
  )

  private fun getDistinctSentenceCount(): Long? = jdbcTemplate.queryForObject<Long>(
    "SELECT COUNT(DISTINCT sentence_uuid) FROM sentence",
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
