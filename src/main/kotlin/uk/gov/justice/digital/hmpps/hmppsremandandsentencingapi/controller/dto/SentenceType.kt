package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.util.UUID

data class SentenceType(
  val sentenceTypeUuid: UUID,
  val description: String,
  val classification: SentenceTypeClassification,
  val hintText: String?,
) {
  companion object {
    fun from(sentenceTypeEntity: SentenceTypeEntity): SentenceType {
      return SentenceType(sentenceTypeEntity.sentenceTypeUuid, sentenceTypeEntity.description, sentenceTypeEntity.classification, sentenceTypeEntity.hintText)
    }
  }
}
