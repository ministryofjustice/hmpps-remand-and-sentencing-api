package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.LocalDateNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import java.time.LocalDate

data class ImmigrationDetention(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val immigrationDetentionRecordType: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val homeOfficeReferenceNumber: String?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @param:JsonSerialize(nullsUsing = LocalDateNullSerializer::class)
  val recordDate: LocalDate?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val noLongerOfInterestReason: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val noLongerOfInterestComment: String?,
)
