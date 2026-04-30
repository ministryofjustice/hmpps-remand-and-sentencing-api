package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.BooleanNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.BooleanSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import java.time.LocalDate

data class Recall(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val recallType: String?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val revocationDate: LocalDate?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val returnToCustodyDate: LocalDate?,
  @param:JsonSerialize(using = BooleanSerializer::class, nullsUsing = BooleanNullSerializer::class)
  val inPrisonOnRevocationDate: Boolean?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val recallSentenceStatus: String?,
)
