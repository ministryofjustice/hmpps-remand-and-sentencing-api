package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate
import java.util.UUID

data class CreateCourtAppearance(
  val courtCaseUuid: String?,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonSetter(nulls = Nulls.SKIP)
  var appearanceUuid: UUID = UUID.randomUUID(),
  val outcomeUuid: UUID?,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val warrantId: String?,
  val warrantType: String,
  val overallSentenceLength: CreatePeriodLength?,
  val nextCourtAppearance: CreateNextCourtAppearance?,
  val charges: List<CreateCharge>,
  val overallConvictionDate: LocalDate?,
  var legacyData: CourtAppearanceLegacyData?,
  val prisonId: String,
  val documents: List<UploadedDocument>?,
)
