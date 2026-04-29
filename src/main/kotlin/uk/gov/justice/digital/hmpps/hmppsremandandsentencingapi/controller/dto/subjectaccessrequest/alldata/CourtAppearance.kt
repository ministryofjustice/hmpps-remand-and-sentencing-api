package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import java.time.LocalDate

data class CourtAppearance(
  @param:JsonSerialize(using = LocalDateSerializer::class, nullsUsing = LocalDateNullSerializer::class)
  val appearanceDate: LocalDate?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val appearanceOutcomeName: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val warrantType: String?,
  @param:JsonSerialize(using = LocalDateSerializer::class, nullsUsing = LocalDateNullSerializer::class)
  val convictionDate: LocalDate?,
  @param:JsonSerialize(using = LocalDateSerializer::class, nullsUsing = LocalDateNullSerializer::class)
  val nextAppearanceDate: LocalDate?,
  val charges: List<Charge>?,
)
