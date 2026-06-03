package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.infrastructure.item.ItemProcessor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.batch.ManyChargesToSentenceFixQueueEntity

class PassThroughProcessor(private val delay: Long) : ItemProcessor<ManyChargesToSentenceFixQueueEntity, ManyChargesToSentenceFixQueueEntity> {
  override fun process(item: ManyChargesToSentenceFixQueueEntity): ManyChargesToSentenceFixQueueEntity {
    Thread.sleep(delay)
    log.debug("Processing Case Identifier {}", item.caseUniqueIdentifier)
    return item
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
