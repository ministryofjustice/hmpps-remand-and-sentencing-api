package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import java.time.LocalDate

data class RecallCourtCaseDetails(
  val courtCaseReference: String?,
  val courtCaseUuid: String?,
  val courtCode: String?,
  val sentencingAppearanceDate: LocalDate?,
  val sentences: List<RecalledSentence> = emptyList(),
)
