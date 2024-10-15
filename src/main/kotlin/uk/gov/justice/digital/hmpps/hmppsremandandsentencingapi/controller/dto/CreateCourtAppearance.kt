package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtAppearanceLegacyData
import java.time.LocalDate
import java.util.UUID

data class CreateCourtAppearance(
  val courtCaseUuid: String?,
  val appearanceUuid: UUID?,
  val outcomeUuid: UUID?,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val warrantId: String?,
  val warrantType: String,
  val taggedBail: Int?,
  val overallSentenceLength: CreatePeriodLength?,
  val nextCourtAppearance: CreateNextCourtAppearance?,
  val charges: List<CreateCharge>,
  val overallConvictionDate: LocalDate?,
  val legacyData: CourtAppearanceLegacyData?,
)
