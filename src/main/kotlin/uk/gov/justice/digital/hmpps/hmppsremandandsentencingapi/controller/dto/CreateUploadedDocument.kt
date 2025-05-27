package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class CreateUploadedDocument(
  val appearanceUUID: UUID?,
  val documents: List<UploadedDocument>,
  val createdBy: String,
)
