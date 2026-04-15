package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.*

data class CreateSentenceType(
  val sentenceTypeUuid: UUID?,
  @field:NotBlank("Description must not be blank")
  val description: String,
  val minAgeInclusive: Int?,
  val maxAgeExclusive: Int?,
  val minDateInclusive: LocalDate?,
  val maxDateExclusive: LocalDate?,
  val minOffenceDateInclusive: LocalDate?,
  val maxOffenceDateExclusive: LocalDate?,
  val classification: SentenceTypeClassification,
  val hintText: String?,
  @field:NotBlank("NOMIS CJA Code must not be blank")
  val nomisCjaCode: String,
  @field:NotBlank("NOMIS Sentence Calc Type must not be blank")
  val nomisSentenceCalcType: String,
  val displayOrder: Int,
  val status: ReferenceEntityStatus,
  val isRecallable: Boolean,
)
