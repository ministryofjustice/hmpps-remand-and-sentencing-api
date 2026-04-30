package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer

data class Sentence(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val sentenceTypeDescription: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val sentenceTypeClassification: String?,
  val periodLengths: List<PeriodLength>?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val sentenceServeType: String?,
)
