package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class RetrieveUploadedDocuments(
  val appearanceUUID: UUID,
  val warrantType: String,
)
