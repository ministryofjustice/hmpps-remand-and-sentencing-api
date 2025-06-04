package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.util.UUID

data class PagedSentenceType(
  val sentenceTypeUuid: UUID,
  val description: String,
  val classification: SentenceTypeClassification,
)
