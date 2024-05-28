package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate
import java.util.UUID

data class CreateCourtAppearance(
  val courtCaseUuid: String?,
  val appearanceUuid: UUID?,
  val outcome: String,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val warrantId: String?,
  val warrantType: String,
  val taggedBail: Int?,
  val overallSentenceLength: CreatePeriodLength?,
  val nextCourtAppearance: CreateNextCourtAppearance?,
  val charges: List<CreateCharge>,
)
