package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "sentence_type")
class SentenceTypeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column(name = "sentence_type_uuid", unique = true)
  val sentenceTypeUuid: UUID,
  val description: String,
  val minAgeInclusive: Int?,
  val maxAgeExclusive: Int?,
  val minDateInclusive: LocalDate?,
  val maxDateExclusive: LocalDate?,
  val minOffenceDateInclusive: LocalDate?,
  val maxOffenceDateExclusive: LocalDate?,
  @Enumerated(EnumType.STRING)
  val classification: SentenceTypeClassification,
  val hintText: String?,
  val nomisCjaCode: String,
  val nomisSentenceCalcType: String,
  val displayOrder: Int,
  @Enumerated(EnumType.STRING)
  val status: ReferenceEntityStatus,
)
