package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.time.LocalDateTime
import java.util.UUID

data class HmctsCourHearing(
  val hearingId: UUID,
  val courtName: String,
  val courtId: UUID,
  val hearingDate: LocalDateTime,
  val caseReferences: List<String>,
  val hearingType: String,
  val documents: List<HmctsCourHearingDocument>,
)

data class HmctsCourHearingDocument(
  val documentType: String,
  val documentId: UUID,
  val ingestionAt: LocalDateTime,
  val filename: String?,
)
