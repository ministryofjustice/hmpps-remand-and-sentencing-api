package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

data class BookingCreateCourtCasesResponse(
  val courtCases: List<BookingCreateCourtCaseResponse>,
  val appearances: List<BookingCreateCourtAppearanceResponse>,
  val charges: List<BookingCreateChargeResponse>,
  val sentences: List<BookingCreateSentenceResponse>,
  val sentenceTerms: List<BookingCreatePeriodLengthResponse>,
)
