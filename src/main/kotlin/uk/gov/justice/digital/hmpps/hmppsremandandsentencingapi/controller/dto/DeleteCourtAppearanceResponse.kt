package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity

data class DeleteCourtAppearanceResponse(
  val records: RecordResponse<CourtAppearanceEntity>,
  val courtCaseUuid: String,
)
