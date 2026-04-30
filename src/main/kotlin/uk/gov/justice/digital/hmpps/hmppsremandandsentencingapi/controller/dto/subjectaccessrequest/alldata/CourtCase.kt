package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.ZonedDateTimeNullSerializer
import java.time.ZonedDateTime

data class CourtCase(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val courtName: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val caseStatus: String?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
  @param:JsonSerialize(nullsUsing = ZonedDateTimeNullSerializer::class)
  val createdAt: ZonedDateTime?,
  @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
  @param:JsonSerialize(nullsUsing = ZonedDateTimeNullSerializer::class)
  val updatedAt: ZonedDateTime?,
  val latestCourtAppearance: CourtAppearance?,
)
