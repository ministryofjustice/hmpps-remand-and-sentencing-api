package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.*
/*
The JSON mapper kicks in before the jakarta validation so if someone were to supply a null value on a required field it results in a 500 unable to json map exception rather than the
validation message from the annotations so have to change the fields to nullable to force the validation messages
 */
data class CreateSentenceType(
  @field:NotNull("You must enter a valid UUID")
  val sentenceTypeUuid: UUID?,
  @field:NotBlank("You must enter a description")
  val description: String?,
  val minAgeInclusive: Int?,
  val maxAgeExclusive: Int?,
  val minDateInclusive: LocalDate?,
  val maxDateExclusive: LocalDate?,
  val minOffenceDateInclusive: LocalDate?,
  val maxOffenceDateExclusive: LocalDate?,
  @field:NotNull("You must select a Classification")
  val classification: SentenceTypeClassification?,
  val hintText: String?,
  @field:NotBlank("You must enter a NOMIS CJA Code")
  val nomisCjaCode: String?,
  @field:NotBlank("You must enter a NOMIS Sentence Calc Type")
  val nomisSentenceCalcType: String?,
  @field:NotNull("You must enter a Display Order")
  val displayOrder: Int?,
  @field:NotNull("You must select a Status")
  val status: ReferenceEntityStatus?,
  @field:NotNull("You must select whether the sentence type is recallable")
  val isRecallable: Boolean?,
)
