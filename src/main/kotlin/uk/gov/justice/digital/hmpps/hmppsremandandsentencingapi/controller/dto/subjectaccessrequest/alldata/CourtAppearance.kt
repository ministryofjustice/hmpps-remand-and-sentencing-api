package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import java.time.LocalDate

data class CourtAppearance(
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val appearanceDate: LocalDate?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val appearanceOutcomeName: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val warrantType: String?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val convictionDate: LocalDate?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val nextAppearanceDate: LocalDate?,
  val charges: List<Charge>?,
)
