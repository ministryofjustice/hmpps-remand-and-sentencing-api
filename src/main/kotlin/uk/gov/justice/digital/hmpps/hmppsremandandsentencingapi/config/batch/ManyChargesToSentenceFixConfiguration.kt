package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.batch

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.listener.JobExecutionListener
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.PlatformTransactionManager
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.batch.ManyChargesToSentenceFixQueueEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.batch.MultipleChargesSingleSentenceFixQueueRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.FixManyChargesToSentenceService

@Configuration
@Profile("batch")
class ManyChargesToSentenceFixConfiguration {

  /**
   * Primary Job:
   *   Step 1 [identifyCourtCasesToRepairStep]   — find affected court cases and stage them in a queue table
   *
   *   Step 2 [processCourtCasesToRepairRecordsStep] — process each staged case one at a time
   */
  @Bean
  fun manyChargesToSentenceFixJob(
    jobRepository: JobRepository,
    identifyCourtCasesToRepairStep: Step,
    processCourtCasesToRepairRecordsStep: Step,
    listeners: List<JobExecutionListener>,
  ): Job? {
    val builder = JobBuilder("manyChargesToSentenceFixJob", jobRepository)
      .start(identifyCourtCasesToRepairStep)
      .next(processCourtCasesToRepairRecordsStep)

    listeners.forEach { builder.listener(it) }

    return builder.build()
  }

  // ---------------------------------------------------------------------------
  // Step 1: Identify court cases to repair
  // ---------------------------------------------------------------------------

  @Bean
  fun identifyCourtCasesToRepairStep(
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager?,
    identifyCourtCasesToRepairTasklet: Tasklet,
  ): Step? = StepBuilder("identifyCourtCasesToRepairStep", jobRepository)
    .tasklet(identifyCourtCasesToRepairTasklet)
    .transactionManager(transactionManager)
    .build()

  /**
   * Truncates the queue table then populates it with court cases whose latest appearance
   * contains sentences marked MANY_CHARGES_DATA_FIX. We stage them to aid visibility,
   * debugging, and re-running if needed.
   */
  @Bean
  fun identifyCourtCasesToRepairTasklet(
    fixQueueRepository: MultipleChargesSingleSentenceFixQueueRepository,
    @Value("\${multiple-charges-single-sentence-fix-queue.limit:500}") limit: Int,
  ): Tasklet = Tasklet { _, _ ->
    fixQueueRepository.truncate()
    fixQueueRepository.populateQueue(limit)
    RepeatStatus.FINISHED
  }

  // ---------------------------------------------------------------------------
  // Step 2: Process staged court cases
  // ---------------------------------------------------------------------------

  @Bean
  fun processCourtCasesToRepairRecordsStep(
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager,
    courtCasesToRepairReader: JpaPagingItemReader<ManyChargesToSentenceFixQueueEntity>,
    courtCasesToRepairProcessor: ItemProcessor<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity>,
    courtCasesToRepairWriter: ItemWriter<ManyChargesToSentenceFixQueueEntity>,
  ): Step = StepBuilder("processCourtCasesToRepairRecordsStep", jobRepository)
    .chunk<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity>(1) // one repair at a time
    .reader(courtCasesToRepairReader)
    .processor(courtCasesToRepairProcessor)
    .writer(courtCasesToRepairWriter)
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
  ): ItemProcessor<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity> = PassThroughProcessor(delay)

  /**
   * Fix takes place here using an existing service. SENTENCE_UPDATED Domain Events are emitted.
   */
  @Bean
  fun courtCasesToRepairWriter(
    fixManyChargesToSentenceService: FixManyChargesToSentenceService,
    courtCaseRepository: CourtCaseRepository,
    dpsDomainEventService: DpsDomainEventService,
  ): ItemWriter<ManyChargesToSentenceFixQueueEntity> = ItemWriter { chunk: Chunk<out ManyChargesToSentenceFixQueueEntity> ->
    for (stagedItem in chunk.getItems()) {
      courtCaseRepository.findByCaseUniqueIdentifier(stagedItem.caseUniqueIdentifier)?.let {
        val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(listOf(it))
        dpsDomainEventService.emitEvents(eventsToEmit)
      }
    }
  }

  @Profile("!test")
  @Bean
  fun jobCompletionNotificationListener(context: ApplicationContext): JobExecutionListener = object : JobExecutionListener {
    override fun afterJob(jobExecution: JobExecution) {
      val exitCode = if (jobExecution.status == BatchStatus.COMPLETED) 0 else 1
      kotlin.system.exitProcess(org.springframework.boot.SpringApplication.exit(context, { exitCode }))
    }
  }
}
