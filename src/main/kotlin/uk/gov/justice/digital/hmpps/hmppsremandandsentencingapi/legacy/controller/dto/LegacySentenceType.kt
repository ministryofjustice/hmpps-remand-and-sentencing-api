package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeDetail
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypePeriodDefinitions
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.SentenceEligibility
import java.time.LocalDate

data class LegacySentenceType(
  @JsonIgnore
  val id: Int,

  val nomisSentenceTypeReference: String,
  val classification: SentenceTypeClassification,
  val classificationPeriodDefinition: SentenceTypePeriodDefinitions?,
  val sentencingAct: Int,
  val eligibility: SentenceEligibility?,
  val recallType: RecallType,
  val inputSentenceType: SentenceTypeDetail?,
  val nomisActive: Boolean,
  val nomisDescription: String,
  val nomisExpiryDate: LocalDate?,
  val nomisTermTypes: Map<String, String>,
)
