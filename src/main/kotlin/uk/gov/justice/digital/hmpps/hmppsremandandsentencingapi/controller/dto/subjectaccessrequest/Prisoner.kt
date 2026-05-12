package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.jdk.StringSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.StringNullSerializer

data class Prisoner(
  @param:JsonSerialize(using = StringSerializer::class, nullsUsing = StringNullSerializer::class)
  override val prisonerNumber: String? = null,
  val immigrationDetentions: List<ImmigrationDetention>? = listOf(),
) : SarContent
