package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class RecallCourtCaseDetails(
  val courtCaseReference: String?,
  val courtCaseUuid: String?,
  val courtCode: String?,
  val sentencingAppearanceDate: LocalDate?,
  @get:Schema(description = "NOMIS booking id from court_case.legacy_data, used to determine period of custody")
  val bookingId: Long? = null,
  val sentences: List<RecalledSentence> = emptyList(),
)
