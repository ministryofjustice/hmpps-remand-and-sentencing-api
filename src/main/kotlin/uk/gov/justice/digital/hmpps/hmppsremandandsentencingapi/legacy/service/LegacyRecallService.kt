package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyRecall
import java.util.UUID

@Service
class LegacyRecallService(
  private val recallRepository: RecallRepository,
) {

  @Transactional(readOnly = true)
  fun get(recallUuid: UUID): LegacyRecall = LegacyRecall.from(getUnlessDeleted(recallUuid))

  private fun getUnlessDeleted(uuid: UUID): RecallEntity = recallRepository.findOneByRecallUuid(uuid)
    ?.takeUnless { entity -> entity.status == RecallEntityStatus.DELETED }
    ?: throw EntityNotFoundException("No recall found at $uuid")

  companion object {
    /* The known mapping of legacy recall sentence types to classifications. Any missing sentence types do not map to a single classification. */
    val classificationToLegacySentenceTypeMap = mapOf(
      SentenceTypeClassification.EXTENDED to listOf(
        "LR_EDS18",
        "LR_EDS21",
        "LR_EDSU18",
      ),
      SentenceTypeClassification.INDETERMINATE to listOf(
        "LR_LIFE",
        "LR_MLP",
        "LR_ALP",
        "LR_ALP_CDE18",
        "LR_ALP_CDE21",
        "LR_DLP",
      ),
      SentenceTypeClassification.SOPC to listOf(
        "LR_SOPC18",
        "LR_SOPC21",
      ),
    )
  }
}
