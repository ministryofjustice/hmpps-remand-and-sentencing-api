package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import java.util.UUID

data class PagedSentenceType(
  val sentenceTypeUuid: UUID,
  val description: String,
)
