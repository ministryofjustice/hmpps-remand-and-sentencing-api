package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

data class MergeCreateCourtCasesResponse(
  val courtCases: List<MergeCreateCourtCaseResponse>,
  val appearances: List<MergeCreateCourtAppearanceResponse>,
  val charges: List<MergeCreateChargeResponse>,
  val sentences: List<MergeCreateSentenceResponse>,
  val sentenceTerms: List<MergeCreatePeriodLengthResponse>,
)
