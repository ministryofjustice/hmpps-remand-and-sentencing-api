package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.batch

import jakarta.persistence.EntityManager
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@Profile("batch")
class MultipleChargesSingleSentenceFixConfiguration {

  /**
   * Prepare the Batch job by identifying the court cases that need to be repaired
   * and staging them in a queue table for processing by subsequent steps.
   * We stage them in a queue table to allow for better visibility of the work to be done
   * and to allow for easier debugging and re-running of the job if needed.
   */
  @Bean
  fun identifyCourtCasesToRepairTasklet(
    em: EntityManager,
    @Value("\${multiple-charges-single-sentence-fix-queue.limit:500}") limit: Int,
  ): Tasklet = Tasklet { contribution: StepContribution?, chunkContext: ChunkContext? ->

    // delete old records from a prior successful job execution
    em.createNativeQuery("TRUNCATE TABLE multiple_charges_single_sentence_fix_queue").executeUpdate()

    em.createNativeQuery(
      """INSERT INTO multiple_charges_single_sentence_fix_queue (case_unique_identifier)
                     SELECT cc.case_unique_identifier
                     FROM court_case cc
                     WHERE EXISTS (
                          SELECT 1
                          FROM court_appearance lcap
                          JOIN appearance_charge ac ON ac.appearance_id = lcap.id
                          JOIN charge c ON c.id = ac.charge_id
                          JOIN sentence s ON s.charge_id = c.id
                          WHERE lcap.id = cc.latest_court_appearance_id
                          AND s.status_id = 'MANY_CHARGES_DATA_FIX'
                     )
                     ORDER BY cc.updated_at DESC
                     LIMIT :limit;                                  
      """.trimIndent(),
    ).setParameter("limit", limit).executeUpdate()

    RepeatStatus.FINISHED
  }

  @Bean
  fun identifyCourtCasesToRepairStep(
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager?,
    stageCourtCasesToRepairTasklet: Tasklet,
  ): Step? = StepBuilder("identifyCourtCasesToRepairStep", jobRepository)
    .tasklet(stageCourtCasesToRepairTasklet)
    .transactionManager(transactionManager)
    .build()

  @Bean
  fun multipleChargesSentenceFixJob(
    jobRepository: JobRepository,
    identifyCourtCasesToRepairStep: Step,
  ): Job? = JobBuilder("multipleChargesSentenceFixJob", jobRepository)
    .start(identifyCourtCasesToRepairStep)
    .build()
}
