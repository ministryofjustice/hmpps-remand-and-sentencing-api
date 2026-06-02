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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

@SpringBatchTest
@ActiveProfiles("batch", "test", "test-batch")
class MultipleChargesSingleSentenceFixTests : IntegrationTestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var jobOperatorTestUtils: JobOperatorTestUtils

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var multipleChargesSentenceFixJob: Job

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
    val params = JobParametersBuilder()
      .addString("run.date", "2026-01-01")
      .toJobParameters()
    jobOperatorTestUtils.setJob(multipleChargesSentenceFixJob)
    val result = jobOperatorTestUtils.startJob(params)

    assertThat(result.status).isEqualTo(BatchStatus.COMPLETED)
  }
}
