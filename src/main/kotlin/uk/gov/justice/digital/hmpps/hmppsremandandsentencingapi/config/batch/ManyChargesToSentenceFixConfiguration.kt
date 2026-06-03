package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.batch

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.PlatformTransactionManager
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.batch.ManyChargesToSentenceFixQueueEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.FixManyChargesToSentenceService
import java.util.*

@Configuration
@Profile("batch")
class ManyChargesToSentenceFixConfiguration {

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
    em.createNativeQuery("TRUNCATE TABLE many_charges_to_sentence_fix_queue").executeUpdate()

    em.createNativeQuery(
      """INSERT INTO many_charges_to_sentence_fix_queue (case_unique_identifier)
                     SELECT cc.case_unique_identifier
                     FROM court_case cc
                     WHERE EXISTS (
                          SELECT 1
                          FROM court_appearance lcap
                          JOIN appearance_charge ac ON ac.appearance_id = lcap.id
                          JOIN charge c ON c.id = ac.charge_id
                          JOIN sentence s ON s.charge_id = c.id
                          -- WHERE lcap.court_case_id = cc.id
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
  fun courtCasesToRepairReader(emf: EntityManagerFactory): JpaPagingItemReader<ManyChargesToSentenceFixQueueEntity> = JpaPagingItemReaderBuilder<ManyChargesToSentenceFixQueueEntity>()
    .name("courtCasesToRepairReader")
    .entityManagerFactory(emf)
    .queryString("SELECT m FROM ManyChargesToSentenceFixQueueEntity m ORDER BY m.id DESC")
    .build()

  @Bean
  fun courtCasesToRepairProcessor(
    @Value("\${multiple-charges-single-sentence-fix-queue.chunk-delay-ms:500}") delay: Long,
  ): ItemProcessor<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity> {
    return PassThroughProcessor(delay)
  }

  @Bean
  fun courtCasesToRepairWriter(
    fixManyChargesToSentenceService: FixManyChargesToSentenceService,
    courtCaseRepository: CourtCaseRepository,
    dpsDomainEventService: DpsDomainEventService,
  ): ItemWriter<ManyChargesToSentenceFixQueueEntity> = ItemWriter { chunk: Chunk<out ManyChargesToSentenceFixQueueEntity> ->
    for (stagedItem in chunk.getItems()) {
      val courtCaseUUID = stagedItem.caseUniqueIdentifier

      courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let {
        val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(listOf(it))
        dpsDomainEventService.emitEvents(eventsToEmit)
      }
    }
  }

  @Bean
  fun processCourtCasesToRepairRecordsStep(
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager,
    courtCasesToRepairReader: JpaPagingItemReader<ManyChargesToSentenceFixQueueEntity>,
    courtCasesToRepairProcessor: ItemProcessor<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity>,
    courtCasesToRepairWriter: ItemWriter<ManyChargesToSentenceFixQueueEntity>,
  ): Step = StepBuilder("processCourtCasesToRepairRecordsStep", jobRepository)
    .chunk<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity>(1) // Make one repair at a time
    .reader(courtCasesToRepairReader)
    .processor(courtCasesToRepairProcessor)
    .writer(courtCasesToRepairWriter)
    .transactionManager(transactionManager)
    .build()

  /**
   * Primary Job container
   * Step 1. Identify Court Cases in need of Repair
   * Step 2. Process Court Cases in need of Repair
   * (with a chunk size of 1 to ensure we only process one court case
   * at a time to avoid potential performance issues and to make it easier
   * to identify any issues with specific court cases)
   */
  @Bean
  fun manyChargesToSentenceFixJob(
    jobRepository: JobRepository,
    identifyCourtCasesToRepairStep: Step,
    processCourtCasesToRepairRecordsStep: Step,
  ): Job? = JobBuilder("manyChargesToSentenceFixJob", jobRepository)
    .start(identifyCourtCasesToRepairStep)
    .next(processCourtCasesToRepairRecordsStep)
    .build()
}
