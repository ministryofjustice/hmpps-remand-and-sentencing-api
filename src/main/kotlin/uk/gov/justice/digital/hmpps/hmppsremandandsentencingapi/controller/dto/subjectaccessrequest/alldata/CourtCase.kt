package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.ZonedDateTimeNullSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.ZonedDateTimeSerializer
import java.time.ZonedDateTime

data class CourtCase(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val courtName: String?,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val caseStatus: String?,
  @param:JsonSerialize(using = ZonedDateTimeSerializer::class, nullsUsing = ZonedDateTimeNullSerializer::class)
  val createdAt: ZonedDateTime?,
  @param:JsonSerialize(using = ZonedDateTimeSerializer::class, nullsUsing = ZonedDateTimeNullSerializer::class)
  val updatedAt: ZonedDateTime?,
  val courtAppearance: CourtAppearance?,
)
