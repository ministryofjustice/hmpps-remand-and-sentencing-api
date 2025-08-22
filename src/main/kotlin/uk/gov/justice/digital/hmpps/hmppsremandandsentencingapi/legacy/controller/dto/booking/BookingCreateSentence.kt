package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate

data class BookingCreateSentence(
  val sentenceId: BookingSentenceId,
  val fine: BookingCreateFine?,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val consecutiveToSentenceId: BookingSentenceId?,
  val periodLengths: List<BookingCreatePeriodLength>,
  val returnToCustodyDate: LocalDate? = null,
)
