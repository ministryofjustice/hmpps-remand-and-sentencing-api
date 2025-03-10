package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationCreateCourtCasesResponse(
  val courtCases: List<MigrationCreateCourtCaseResponse>,
  val appearances: List<MigrationCreateCourtAppearanceResponse>,
  val charges: List<MigrationCreateChargeResponse>,
  val sentences: List<MigrationCreateSentenceResponse>,
  val sentenceTerms: List<MigrationCreatePeriodLengthResponse>,
)
