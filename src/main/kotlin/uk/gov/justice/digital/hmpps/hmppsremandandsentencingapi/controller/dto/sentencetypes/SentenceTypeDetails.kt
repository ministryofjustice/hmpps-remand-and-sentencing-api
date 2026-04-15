package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.*

data class SentenceTypeDetails(
  val sentenceTypeUuid: UUID,
  val description: String,
  val minAgeInclusive: Int?,
  val maxAgeExclusive: Int?,
  val minDateInclusive: LocalDate?,
  val maxDateExclusive: LocalDate?,
  val minOffenceDateInclusive: LocalDate?,
  val maxOffenceDateExclusive: LocalDate?,
  val classification: SentenceTypeClassification,
  val hintText: String?,
  val nomisCjaCode: String,
  val nomisSentenceCalcType: String,
  val displayOrder: Int,
  val status: ReferenceEntityStatus,
  val isRecallable: Boolean,
) {
  companion object {
    fun from(sentenceTypeEntity: SentenceTypeEntity): SentenceTypeDetails = SentenceTypeDetails(
      sentenceTypeEntity.sentenceTypeUuid,
      sentenceTypeEntity.description,
      sentenceTypeEntity.minAgeInclusive,
      sentenceTypeEntity.maxAgeExclusive,
      sentenceTypeEntity.minDateInclusive,
      sentenceTypeEntity.maxDateExclusive,
      sentenceTypeEntity.minOffenceDateInclusive,
      sentenceTypeEntity.maxOffenceDateExclusive,
      sentenceTypeEntity.classification,
      sentenceTypeEntity.hintText,
      sentenceTypeEntity.nomisCjaCode,
      sentenceTypeEntity.nomisSentenceCalcType,
      sentenceTypeEntity.displayOrder,
      sentenceTypeEntity.status,
      sentenceTypeEntity.isRecallable,
    )
  }
}
