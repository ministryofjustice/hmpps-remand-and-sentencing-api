package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import java.util.UUID

data class BookingCreateSentenceResponse(
  val sentenceUuid: UUID,
  val sentenceNOMISId: BookingSentenceId,
)
