package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

data class DocumentMetadata(
  val prisonerId: String,
  val source: String,
  val status: String,
)
