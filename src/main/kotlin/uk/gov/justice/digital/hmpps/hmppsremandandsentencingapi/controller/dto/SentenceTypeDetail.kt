package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.UUID

data class SentenceTypeDetail(
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
) {
  companion object {
    fun from(entity: SentenceTypeEntity): SentenceTypeDetail = SentenceTypeDetail(
      sentenceTypeUuid = entity.sentenceTypeUuid,
      description = entity.description,
      minAgeInclusive = entity.minAgeInclusive,
      maxAgeExclusive = entity.maxAgeExclusive,
      minDateInclusive = entity.minDateInclusive,
      maxDateExclusive = entity.maxDateExclusive,
      minOffenceDateInclusive = entity.minOffenceDateInclusive,
      maxOffenceDateExclusive = entity.maxOffenceDateExclusive,
      classification = entity.classification,
      hintText = entity.hintText,
      nomisCjaCode = entity.nomisCjaCode,
      nomisSentenceCalcType = entity.nomisSentenceCalcType,
      displayOrder = entity.displayOrder,
      status = entity.status,
    )
  }
}
