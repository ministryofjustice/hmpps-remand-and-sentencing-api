package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import java.time.LocalDate
import java.util.UUID

data class CreateSentence(
  val sentenceUuid: UUID,
  val chargeNumber: String?,
  val periodLengths: List<CreatePeriodLength>,
  val sentenceServeType: String,
  val consecutiveToSentenceUuid: UUID?,
  val sentenceTypeId: UUID?,
  val convictionDate: LocalDate?,
  val fineAmount: CreateFineAmount?,
  val prisonId: String?,
  val status: SentenceEntityStatus? = null,
  val reason: String? = null,
)
