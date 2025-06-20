package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

data class Offence(
  val id: Long,
  val code: String,
  val description: String? = null,
  val offenceType: String? = null,
  val revisionId: Int,
  val startDate: String,
  val endDate: String? = null,
)