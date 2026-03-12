package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository

@Service
class MigrateSentenceRecordsSentenceType(private val sentenceRepository: SentenceRepository) {

  @Async
  @Transactional
  fun migrateSentenceRecordsSentenceType(sentenceTypeEntity: SentenceTypeEntity) {
    log.info("starting migrating sentence records with nomis cja code ${sentenceTypeEntity.nomisCjaCode} and nomis sentence calc type ${sentenceTypeEntity.nomisSentenceCalcType}")
    sentenceRepository.updateToSupportedSentenceType(sentenceTypeEntity.id, sentenceTypeEntity.nomisCjaCode, sentenceTypeEntity.nomisSentenceCalcType)
    log.info("finished migrating sentence records with nomis cja code ${sentenceTypeEntity.nomisCjaCode} and nomis sentence calc type ${sentenceTypeEntity.nomisSentenceCalcType}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
