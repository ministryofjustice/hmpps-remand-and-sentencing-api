package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import java.time.LocalDateTime

data class CaseReferenceLegacyData(
  val offenderCaseReference: String,
  val updatedDate: LocalDateTime,
  @Enumerated(EnumType.STRING)
  val source: EventSource? = null,
)
