package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.BooleanNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.BooleanSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import java.time.LocalDate

data class Charge(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val offenceCode: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val offenceDescription: String?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val terrorRelated: Boolean?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val foreignPowerRelated: Boolean?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val domesticViolenceRelated: Boolean?,

  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val offenceStartDate: LocalDate?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val offenceEndDate: LocalDate?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val chargeOutcome: String?,
  val liveSentence: Sentence?,
)
