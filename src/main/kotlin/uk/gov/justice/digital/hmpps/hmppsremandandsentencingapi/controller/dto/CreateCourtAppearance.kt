package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtAppearanceLegacyData
import java.time.LocalDate
import java.util.UUID

data class CreateCourtAppearance(
  val courtCaseUuid: String?,
  @JsonSetter(nulls = Nulls.SKIP)
  var appearanceUuid: UUID = UUID.randomUUID(),
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
