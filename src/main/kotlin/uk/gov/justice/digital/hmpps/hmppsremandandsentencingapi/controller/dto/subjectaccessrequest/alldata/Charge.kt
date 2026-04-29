package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.BooleanNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.BooleanSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import java.time.LocalDate

data class Charge(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val offenseCode: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val offenseDescription: String?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val terrorRelated: Boolean?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val foreignPowerRelated: Boolean?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val domesticViolenceRelated: Boolean?,
  @param:JsonSerialize(using = LocalDateSerializer::class, nullsUsing = LocalDateNullSerializer::class)
  val offenseStartDate: LocalDate?,
  @param:JsonSerialize(using = LocalDateSerializer::class, nullsUsing = LocalDateNullSerializer::class)
  val offenseEndDate: LocalDate?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val chargeOutcome: String?,
  val sentences: List<Sentence>?,
)
