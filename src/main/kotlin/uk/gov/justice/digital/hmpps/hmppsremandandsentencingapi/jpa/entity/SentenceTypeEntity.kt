package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes.CreateSentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "sentence_type")
@DynamicUpdate
class SentenceTypeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column(name = "sentence_type_uuid", unique = true)
  var sentenceTypeUuid: UUID,
  var description: String,
  var minAgeInclusive: Int?,
  var maxAgeExclusive: Int?,
  var minDateInclusive: LocalDate?,
  var maxDateExclusive: LocalDate?,
  var minOffenceDateInclusive: LocalDate?,
  var maxOffenceDateExclusive: LocalDate?,
  @Enumerated(EnumType.STRING)
  var classification: SentenceTypeClassification,
  var hintText: String?,
  var nomisCjaCode: String,
  var nomisSentenceCalcType: String,
  var displayOrder: Int,
  @Enumerated(EnumType.STRING)
  var status: ReferenceEntityStatus,
  @Column(name = "is_recallable")
  var isRecallable: Boolean,
) {

  fun updateFrom(existingUuid: UUID, updateSentenceType: CreateSentenceType) {
    this.sentenceTypeUuid = updateSentenceType.sentenceTypeUuid ?: existingUuid
    this.description = updateSentenceType.description
    this.minAgeInclusive = updateSentenceType.minAgeInclusive
    this.maxAgeExclusive = updateSentenceType.maxAgeExclusive
    this.minDateInclusive = updateSentenceType.minDateInclusive
    this.maxDateExclusive = updateSentenceType.maxDateExclusive
    this.minOffenceDateInclusive = updateSentenceType.minOffenceDateInclusive
    this.maxOffenceDateExclusive = updateSentenceType.maxOffenceDateExclusive
    this.classification = updateSentenceType.classification
    this.hintText = updateSentenceType.hintText
    this.nomisCjaCode = updateSentenceType.nomisCjaCode
    this.nomisSentenceCalcType = updateSentenceType.nomisSentenceCalcType
    this.displayOrder = updateSentenceType.displayOrder
    this.status = updateSentenceType.status
    this.isRecallable = updateSentenceType.isRecallable
  }
  companion object {
    fun from(createSentenceType: CreateSentenceType): SentenceTypeEntity = SentenceTypeEntity(
      0,
      createSentenceType.sentenceTypeUuid ?: UUID.randomUUID(),
      createSentenceType.description,
      createSentenceType.minAgeInclusive,
      createSentenceType.maxAgeExclusive,
      createSentenceType.minDateInclusive,
      createSentenceType.maxDateExclusive,
      createSentenceType.minOffenceDateInclusive,
      createSentenceType.maxOffenceDateExclusive,
      createSentenceType.classification,
      createSentenceType.hintText,
      createSentenceType.nomisCjaCode,
      createSentenceType.nomisSentenceCalcType,
      createSentenceType.displayOrder,
      createSentenceType.status,
      createSentenceType.isRecallable,
    )
  }
}
