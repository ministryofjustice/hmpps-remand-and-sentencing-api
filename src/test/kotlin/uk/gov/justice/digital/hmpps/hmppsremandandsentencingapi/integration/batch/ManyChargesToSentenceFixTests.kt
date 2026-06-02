package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.test.JobOperatorTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.util.UUID

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
  fun `should start batch job successfully`() {
    val sentenceUuid = arrange()

    val params = JobParametersBuilder()
      .addString("run.date", "2026-01-02")
      .toJobParameters()
    jobOperatorTestUtils.setJob(manyChargesToSentenceFixTests)
    val result = jobOperatorTestUtils.startJob(params)
    val sentencesAfterBatchRun = sentenceRepository.findBySentenceUuid(sentenceUuid)

    assertThat(result.status).isEqualTo(BatchStatus.COMPLETED)
    assertThat(sentencesAfterBatchRun).hasSize(1)
  }

  private fun arrange(totalChargesToSentence: Long = 50): UUID {
    val sentence = DataCreator.migrationCreateSentence()
    val charges = mutableListOf<MigrationCreateCharge>()
    (1L..totalChargesToSentence).forEach { i ->
      charges.add(DataCreator.migrationCreateCharge(chargeNOMISId = i, sentence = sentence))
    }
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = charges)
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))
    val response = webTestClient
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

    val sentenceUuid = response.sentences.first().sentenceUuid
    val sentencesForUuid = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesForUuid).hasSize(50)

    return sentenceUuid
  }
}
