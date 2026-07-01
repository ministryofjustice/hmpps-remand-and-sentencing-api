package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.util.UUID

data class DocumentManagementApiDocument(
  val documentUuid: UUID,
  val documentFilename: String,
)
