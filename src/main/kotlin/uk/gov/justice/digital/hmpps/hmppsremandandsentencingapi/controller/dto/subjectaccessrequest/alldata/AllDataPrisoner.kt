package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer

data class AllDataPrisoner(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val prisonerNumber: String? = null,
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  val prisonerName: String? = null,
  val courtCases: List<CourtCase>? = listOf(),
  val recalls: List<Recall>? = listOf(),
  val immigrationDetentions: List<ImmigrationDetention>? = listOf(),
)
